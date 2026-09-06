package dev.codedrill.judge.runner.adapter

import dev.codedrill.judge.protocol.ExecutionMode
import dev.codedrill.judge.protocol.ExecutionRequest
import dev.codedrill.judge.protocol.FencingToken
import dev.codedrill.judge.protocol.Language
import dev.codedrill.judge.protocol.RequestedGroup
import dev.codedrill.judge.runner.execution.adapter.JavaAdapter
import dev.codedrill.judge.runner.execution.adapter.KotlinAdapter
import dev.codedrill.judge.runner.execution.adapter.PythonAdapter
import dev.codedrill.judge.runner.execution.adapter.RuntimeAdapter
import dev.codedrill.judge.runner.execution.adapter.TraceApi
import dev.codedrill.platform.problempackage.ProblemPackageLoader
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * 계측 SDK 표면 검사 (기술 설계서 §0.3, §14.3 스키마 호환).
 *
 * 세 언어가 같은 메서드를 갖는지, 두 모드에서 시그니처가 같은지를 생성물 자체로 확인한다.
 * 갈라지면 같은 풀이가 언어에 따라 다른 리플레이를 내거나, 계측을 넣은 코드가 채점에서만
 * 컴파일 실패한다.
 */
class SdkSurfaceTest {

    private val pkg = ProblemPackageLoader(Path.of("../../content/problems")).load("two-sum")

    @Test
    fun `세 언어가 SDK 의 모든 메서드를 노출한다`() {
        for (adapter in listOf(KotlinAdapter(), JavaAdapter(), PythonAdapter())) {
            for (mode in listOf(ExecutionMode.JUDGE, ExecutionMode.TRACE)) {
                val source = drillSource(adapter, mode)
                val missing = TraceApi.methods.map { it.name }.filterNot { source.contains(it) }

                assertTrue(
                    missing.isEmpty(),
                    "${adapter.language}/$mode 에 빠진 메서드: $missing",
                )
            }
        }
    }

    @Test
    fun `SDK 이름이 어느 언어의 예약어와도 겹치지 않는다`() {
        val offenders = TraceApi.methods.flatMap { method ->
            (listOf(method.name) + method.params.map { it.name })
                .filter { it in TraceApi.reservedWords }
                .map { "${method.name}: $it" }
        }

        assertTrue(
            offenders.isEmpty(),
            "예약어와 겹치면 그 언어에서 SDK 파일 자체가 문법 오류가 된다: $offenders",
        )
    }

    @Test
    fun `판정 모드 SDK 는 이벤트를 내보내지 않는다`() {
        for (adapter in listOf(KotlinAdapter(), JavaAdapter(), PythonAdapter())) {
            val source = drillSource(adapter, ExecutionMode.JUDGE)

            assertTrue(
                !source.contains("EVENT"),
                "${adapter.language}: 판정 모드에 계측 코드가 남아 있다 (§7.1)",
            )
        }
    }

    /** 어댑터가 실제로 써 놓은 SDK 파일을 그대로 읽는다. 생성 로직을 따로 흉내 내지 않는다. */
    private fun drillSource(adapter: RuntimeAdapter, mode: ExecutionMode): String {
        val dir = createTempDirectory("sdk-surface").resolve("src").also { it.createDirectories() }
        adapter.prepare(request(adapter.language, mode), dir)
        val name = when (adapter.language) {
            Language.KOTLIN -> "Drill.kt"
            Language.JAVA -> "Drill.java"
            Language.PYTHON -> "drill.py"
        }
        return dir.resolve(name).readText().also { dir.parent.toFile().deleteRecursively() }
    }

    private fun request(language: Language, mode: ExecutionMode) = ExecutionRequest(
        executionId = "exec-sdk",
        submissionId = "sub-sdk",
        attempt = 1,
        fencingToken = FencingToken(1),
        correlationId = "corr-sdk",
        problemVersionId = pkg.problemVersionId,
        packageDigest = pkg.packageDigest,
        language = language,
        source = "",
        signature = pkg.manifest.signature,
        limits = pkg.manifest.limits,
        groups = pkg.groups.map { RequestedGroup(it.policy, it.cases) },
        mode = mode,
    )
}
