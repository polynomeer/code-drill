package dev.codedrill.judge.runner.content

import dev.codedrill.judge.protocol.ExecutionRequest
import dev.codedrill.judge.protocol.FencingToken
import dev.codedrill.judge.protocol.Language
import dev.codedrill.judge.protocol.RequestedGroup
import dev.codedrill.judge.protocol.Verdict
import dev.codedrill.judge.runner.execution.ExecutionEngine
import dev.codedrill.judge.runner.execution.adapter.KotlinAdapter
import dev.codedrill.judge.runner.execution.sandbox.ProcessSandbox
import dev.codedrill.platform.problempackage.ProblemPackageLoader
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * 콘텐츠 검증 파이프라인 (기술 설계서 §6.3).
 *
 * "모든 공식 해답을 전체 테스트에 실행한다"를 상시 검사로 만든다. 이 검사가 없던 동안
 * `island-count` 의 공개 샘플 하나가 틀린 기대값을 갖고 있었고, 정답 풀이가 오답 판정을
 * 받았다. **틀린 테스트 데이터는 사용자가 자기 코드를 의심하게 만들므로**, 콘텐츠를
 * 추가할 때마다 자동으로 걸러야 한다.
 *
 * 문제를 하나 추가하면 이 테스트가 자동으로 그 문제도 검사한다 — 목록을 따로 관리하지
 * 않는다.
 */
class ContentValidationTest {

    private val contentRoot = Path.of("../../content/problems")
    private val loader = ProblemPackageLoader(contentRoot)

    private val engine = ExecutionEngine(
        adapters = mapOf(Language.KOTLIN to KotlinAdapter()),
        sandboxes = { ProcessSandbox() },
    )

    @Test
    fun `모든 문제의 정답 풀이가 전체 테스트를 통과한다`() {
        val problems = contentRoot.listDirectoryEntries()
            .filter { it.isDirectory() && it.resolve("manifest.yaml").toFile().exists() }
            .map { it.name }
            .sorted()

        assertTrue(problems.isNotEmpty(), "검사할 문제를 찾지 못했다: $contentRoot")

        val failures = problems.mapNotNull { problemId ->
            val pkg = loader.load(problemId)
            val solution = contentRoot.resolve(problemId).resolve("solutions/reference.kt")
            if (!solution.toFile().exists()) {
                return@mapNotNull "$problemId: solutions/reference.kt 가 없다 (§6.1)"
            }

            val result = engine.execute(
                ExecutionRequest(
                    executionId = "content-$problemId",
                    submissionId = "content-$problemId",
                    attempt = 1,
                    fencingToken = FencingToken(1),
                    correlationId = "content",
                    problemVersionId = pkg.problemVersionId,
                    packageDigest = pkg.packageDigest,
                    language = Language.KOTLIN,
                    source = solution.readText(),
                    signature = pkg.manifest.signature,
                    limits = pkg.manifest.limits,
                    groups = pkg.groups.map { RequestedGroup(it.policy, it.cases) },
                ),
            )

            val expectedCases = pkg.groups.sumOf { it.cases.size }
            when {
                result.terminalVerdict != null ->
                    "$problemId: ${result.terminalVerdict} — ${result.compileLog?.lines()?.first()}"

                result.cases.size != expectedCases ->
                    "$problemId: 실행된 케이스가 ${result.cases.size}/$expectedCases 다"

                else -> result.cases.filter { it.verdict != Verdict.ACCEPTED }
                    .takeIf { it.isNotEmpty() }
                    ?.let { failed ->
                        "$problemId: " + failed.joinToString { "${it.groupId}/${it.caseId}=${it.verdict}" }
                    }
            }
        }

        assertTrue(
            failures.isEmpty(),
            "정답 풀이가 자기 테스트를 통과하지 못한다:\n" + failures.joinToString("\n"),
        )
    }
}
