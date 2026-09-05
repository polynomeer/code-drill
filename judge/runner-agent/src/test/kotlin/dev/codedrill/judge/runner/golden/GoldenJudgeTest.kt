package dev.codedrill.judge.runner.golden

import dev.codedrill.judge.protocol.ExecutionMode
import dev.codedrill.judge.protocol.ExecutionRequest
import dev.codedrill.judge.protocol.ExecutionResult
import dev.codedrill.judge.protocol.FencingToken
import dev.codedrill.judge.protocol.Language
import dev.codedrill.judge.protocol.RequestedGroup
import dev.codedrill.judge.protocol.Verdict
import dev.codedrill.judge.runner.execution.ExecutionEngine
import dev.codedrill.judge.runner.execution.adapter.JavaAdapter
import dev.codedrill.judge.runner.execution.adapter.KotlinAdapter
import dev.codedrill.judge.runner.execution.adapter.PythonAdapter
import dev.codedrill.judge.runner.execution.sandbox.ProcessSandbox
import dev.codedrill.platform.problempackage.Limits
import dev.codedrill.platform.problempackage.ProblemPackage
import dev.codedrill.platform.problempackage.ProblemPackageLoader
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 골든 판정 스위트 (기술 설계서 §14.1 Golden Judge, §14.2 코퍼스).
 *
 * 세 언어가 같은 문제·같은 테스트에 대해 같은 판정을 내는지 확인한다. 언어를 늘릴 때
 * 판정 의미가 조용히 갈라지는 것을 막는 것이 이 스위트의 목적이다.
 *
 * 프로세스 샌드박스로 돈다. 격리 자체의 검증은 [dev.codedrill.judge.runner.sandbox]
 * 쪽 회귀 스위트가 맡는다.
 */
class GoldenJudgeTest {

    private val pkg: ProblemPackage =
        ProblemPackageLoader(Path.of("../../content/problems")).load("two-sum")

    private val engine = ExecutionEngine(
        adapters = listOf(KotlinAdapter(), JavaAdapter(), PythonAdapter())
            .associateBy { it.language },
        sandboxes = { ProcessSandbox() },
    )

    @Test
    fun `언어 × 판정 행렬이 전부 일치한다`() {
        val adapters = listOf(KotlinAdapter(), JavaAdapter(), PythonAdapter()).associateBy { it.language }

        // 프로세스 샌드박스에서 메모리 상한을 강제하지 못하는 언어는 이 스위트가 검증할 수
        // 없다. 건너뛰는 대신 통과시키면 격리가 없는 상태를 초록불로 덮게 된다. 해당 칸은
        // ContainerSandboxTest 가 컨테이너 위에서 검증한다.
        val skipped = GoldenSources.matrix().filter {
            it.expected == Verdict.MEMORY_LIMIT &&
                !adapters.getValue(it.language).enforcesMemoryWithoutContainer()
        }
        skipped.forEach {
            println("건너뜀 — ${it.language} / ${it.scenario}: 이 플랫폼은 컨테이너 없이 메모리 상한을 강제하지 못한다")
        }

        val mismatches = (GoldenSources.matrix() - skipped.toSet()).mapNotNull { case ->
            val limits = limitsFor(case.expected)
            val result = engine.execute(request(case.language, case.source, limits))
            val actual = verdictOf(result)
            if (actual == case.expected) {
                null
            } else {
                "${case.language} / ${case.scenario}: $actual (기대 ${case.expected})" +
                    (result.compileLog?.let { " — ${it.lines().first().take(120)}" } ?: "")
            }
        }

        assertTrue(mismatches.isEmpty(), "골든 판정 불일치:\n" + mismatches.joinToString("\n"))
    }

    @Test
    fun `세 언어의 정답 풀이가 모든 케이스를 통과한다`() {
        for (language in GoldenSources.languages) {
            val result = engine.execute(request(language, GoldenSources.ACCEPTED.getValue(language)))

            assertEquals(8, result.cases.size, "$language: 모든 그룹의 케이스가 실행되어야 한다")
            assertTrue(
                result.cases.all { it.verdict == Verdict.ACCEPTED },
                "$language 실패: " + result.cases.filter { it.verdict != Verdict.ACCEPTED },
            )
        }
    }

    @Test
    fun `계측 호출은 판정 실행에서 no-op 이고 트레이스 모드에서만 이벤트를 남긴다`() {
        for (language in GoldenSources.languages) {
            val source = GoldenSources.ACCEPTED.getValue(language)

            val judged = engine.execute(request(language, source))
            assertEquals(null, judged.trace, "$language: 판정 모드는 트레이스를 만들지 않는다")

            val traced = engine.execute(request(language, source, mode = ExecutionMode.TRACE))
            val capture = assertNotNull(traced.trace, "$language: 트레이스가 있어야 한다")
            assertTrue(capture.events.isNotEmpty(), "$language: 계측 이벤트가 있어야 한다")
            assertTrue(
                traced.cases.all { it.groupId == "sample" },
                "$language: 숨은 그룹은 트레이스에서 실행되지 않는다",
            )
        }
    }

    @Test
    fun `컴파일 실패는 케이스를 한 건도 만들지 않고 서버 경로를 노출하지 않는다`() {
        for (language in GoldenSources.languages) {
            val result =
                engine.execute(request(language, GoldenSources.COMPILE_ERROR.getValue(language)))

            assertEquals(Verdict.COMPILE_ERROR, result.terminalVerdict, "$language")
            assertTrue(result.cases.isEmpty(), "$language: 컴파일 실패 시 케이스 결과가 없어야 한다")
            val log = assertNotNull(result.compileLog, "$language: 로그가 있어야 한다")
            assertTrue(
                !log.contains("/private/") && !log.contains("/var/folders/"),
                "$language: 컴파일 로그에 서버 경로가 새면 안 된다: $log",
            )
        }
    }

    @Test
    fun `같은 입력은 언어와 무관하게 같은 resultDigest 를 낸다`() {
        val digests = GoldenSources.languages.map { language ->
            engine.execute(request(language, GoldenSources.ACCEPTED.getValue(language))).resultDigest
        }

        assertEquals(1, digests.distinct().size, "판정이 같으면 digest 도 같아야 한다: $digests")
    }

    // --- 픽스처 ---

    /** 판정별로 실패를 빠르게 만드는 제한. 무한 루프 케이스를 기본 2초로 두면 느려진다. */
    private fun limitsFor(expected: Verdict): Limits = when (expected) {
        Verdict.TIME_LIMIT -> Limits(timeMillis = 700, memoryMb = 256, outputBytes = 65_536)
        Verdict.MEMORY_LIMIT -> Limits(timeMillis = 10_000, memoryMb = 64, outputBytes = 65_536)
        Verdict.OUTPUT_LIMIT -> Limits(timeMillis = 10_000, memoryMb = 256, outputBytes = 1_024)
        else -> pkg.manifest.limits
    }

    private fun verdictOf(result: ExecutionResult): Verdict =
        result.terminalVerdict ?: result.cases.firstOrNull { it.verdict != Verdict.ACCEPTED }?.verdict
            ?: Verdict.ACCEPTED

    private fun request(
        language: Language,
        source: String,
        limits: Limits = pkg.manifest.limits,
        mode: ExecutionMode = ExecutionMode.JUDGE,
    ) = ExecutionRequest(
        executionId = "exec-golden",
        submissionId = "sub-golden",
        attempt = 1,
        fencingToken = FencingToken(1),
        correlationId = "corr-golden",
        problemVersionId = pkg.problemVersionId,
        packageDigest = pkg.packageDigest,
        language = language,
        source = source,
        signature = pkg.manifest.signature,
        limits = limits,
        groups = pkg.groups.map { RequestedGroup(it.policy, it.cases) },
        mode = mode,
    )
}
