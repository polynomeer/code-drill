package dev.codedrill.judge.runner.execution

import dev.codedrill.judge.protocol.ExecutionRequest
import dev.codedrill.judge.protocol.FencingToken
import dev.codedrill.judge.protocol.Language
import dev.codedrill.judge.protocol.RequestedGroup
import dev.codedrill.judge.protocol.Verdict
import dev.codedrill.judge.runner.execution.adapter.PythonAdapter
import dev.codedrill.judge.runner.execution.sandbox.ProcessSandbox
import dev.codedrill.platform.problempackage.ProblemPackageLoader
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 판정 실행이 **공개 그룹에서만** 실제 출력을 싣는가 (§8.3, docs/ui-overhaul.md §6.3).
 *
 * 오답의 1차 정보는 입력·기댓값·실행값이다 (UI 디자인 문서 §4.2). 공개 예제는 입력과 기댓값이
 * 이미 지문에 있으므로 실행값을 보여도 새는 것이 없다. 숨은 그룹의 출력은 판정과 함께 보면 숨은
 * 입력을 되짚을 수 있어 싣지 않는다 — 이 시험은 그 경계를 지킨다.
 */
class PublicOutputTest {

    private val engine = ExecutionEngine(
        adapters = listOf(PythonAdapter()).associateBy { it.language },
        sandboxes = { ProcessSandbox() },
    )

    @Test
    fun `오답의 공개 예제 출력은 싣고 숨은 그룹의 출력은 싣지 않는다`() {
        val pkg = ProblemPackageLoader(Path.of("../../content/problems")).load("two-sum")
        val result = engine.execute(
            ExecutionRequest(
                executionId = "exec-public-output",
                submissionId = "sub-public-output",
                attempt = 1,
                fencingToken = FencingToken(1),
                correlationId = "corr-public-output",
                problemVersionId = pkg.problemVersionId,
                packageDigest = pkg.packageDigest,
                language = Language.PYTHON,
                source = "def twoSum(nums, target):\n    return [0, 0]\n",
                signature = pkg.manifest.signature,
                limits = pkg.manifest.limits,
                groups = pkg.groups.map { RequestedGroup(it.policy, it.cases) },
            ),
        )

        val public = pkg.groups.filter { it.policy.exposesInput }.map { it.policy.id }.toSet()
        val (shown, hidden) = result.cases.partition { it.groupId in public }

        assertTrue(shown.isNotEmpty(), "공개 그룹 결과가 있어야 시험이 뜻을 갖는다")
        val wrong = shown.first { it.verdict == Verdict.WRONG_ANSWER }
        // 하네스의 전송 형식 그대로다 (RuntimeAdapter.encode — INT_ARRAY 는 쉼표로 잇는다). 화면이 되돌린다.
        assertEquals("0,0", wrong.actual, "공개 예제의 실행값이 실려야 한다")
        hidden.forEach { assertNull(it.actual, "숨은 그룹 ${it.groupId}/${it.caseId} 의 출력이 실렸다") }
    }
}
