package dev.codedrill.judge.runner.execution

import dev.codedrill.judge.protocol.ExecutionRequest
import dev.codedrill.judge.protocol.ExecutionResult
import dev.codedrill.judge.protocol.Measurements
import dev.codedrill.judge.protocol.MutationRequest
import dev.codedrill.judge.protocol.MutationStatus
import dev.codedrill.judge.protocol.TestCaseResult
import dev.codedrill.judge.protocol.Verdict
import dev.codedrill.platform.problempackage.DefectKind
import dev.codedrill.platform.problempackage.Limits
import dev.codedrill.platform.problempackage.MutantSource
import dev.codedrill.platform.problempackage.Parameter
import dev.codedrill.platform.problempackage.Signature
import dev.codedrill.platform.problempackage.TestCase
import dev.codedrill.platform.problempackage.ValueType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 변이 평가의 판단 (PRD FR-804).
 *
 * 샌드박스 없이 돈다. 여기서 시험하는 것은 코드가 실제로 실행되는지가 아니라 **결과를
 * 어떻게 읽는가**이고, 그 판단이 틀리면 실행이 아무리 정확해도 사용자에게 나가는 점수가
 * 거짓이 된다.
 *
 * 가장 미끄러운 곳은 **번호 되메우기**다. 기대가 틀린 케이스를 빼고 오답을 돌리므로 결과
 * 목록은 원래 목록보다 짧아지는데, 사용자에게 "3번 케이스가 잡았다"고 말하려면 그 3 은
 * **원래 목록에서의 번호**여야 한다. 빼먹으면 엉뚱한 케이스를 가리키고, 사용자는 자기가
 * 적지도 않은 케이스가 결함을 잡았다고 읽는다.
 */
class MutationEvaluatorTest {

    @Test
    fun `기대가 틀린 케이스를 빼고도 원래 번호로 말한다`() {
        // 2번 케이스의 기대가 틀렸다. 오답은 그 다음 케이스에서만 잡힌다.
        val evaluator = MutationEvaluator { request ->
            when (request.source) {
                REFERENCE -> result(Verdict.ACCEPTED, Verdict.WRONG_ANSWER, Verdict.ACCEPTED)
                // 검산을 통과한 두 건만 온다 — 원래의 1번과 3번이다.
                else -> result(Verdict.ACCEPTED, Verdict.WRONG_ANSWER)
            }
        }

        val report = evaluator.evaluate(request(cases = 3, mutants = listOf(DefectKind.OFF_BY_ONE)))

        assertEquals(MutationStatus.COMPLETED, report.status)
        assertEquals(listOf(2), report.mistakenCases)
        assertTrue(report.mutants.single().killed)
        // 되메우지 않으면 여기가 2 로 나온다 — 사용자가 틀렸다고 들은 바로 그 케이스다.
        assertEquals(listOf(3), report.mutants.single().killedBy)
    }

    @Test
    fun `성능 결함은 점수에서 빠진다`() {
        val evaluator = MutationEvaluator { request ->
            if (request.source == REFERENCE) result(Verdict.ACCEPTED) else result(Verdict.ACCEPTED)
        }

        val report = evaluator.evaluate(
            request(cases = 1, mutants = listOf(DefectKind.PERFORMANCE, DefectKind.OFF_BY_ONE)),
        )

        // 둘 다 살아남았는데 점수는 0/1 이다. 손으로 적은 케이스로 잡을 수 없는 것을
        // 세면 아무도 100% 를 받을 수 없다.
        assertEquals(0.0, report.score())
        assertEquals(2, report.mutants.size)
    }

    @Test
    fun `기대가 전부 틀리면 오답을 돌리지 않는다`() {
        var runs = 0
        val evaluator = MutationEvaluator { _ ->
            runs++
            result(Verdict.WRONG_ANSWER, Verdict.WRONG_ANSWER)
        }

        val report = evaluator.evaluate(request(cases = 2, mutants = listOf(DefectKind.OFF_BY_ONE)))

        // 정답 한 번으로 끝난다. 돌려 봐야 전부 "잡혔다"로 나오는데, 그 잡음의 원인은
        // 오답이 아니라 사용자의 틀린 기대다.
        assertEquals(1, runs)
        assertEquals(listOf(1, 2), report.mistakenCases)
        assertTrue(report.mutants.isEmpty())
        assertEquals(null, report.score())
    }

    @Test
    fun `정답이 돌지 않으면 사용자를 벌하지 않는다`() {
        val evaluator = MutationEvaluator { ExecutionResult(
            executionId = "x", submissionId = "x", attempt = 1,
            fencingToken = dev.codedrill.judge.protocol.FencingToken(1),
            terminalVerdict = Verdict.COMPILE_ERROR, compileLog = "터졌다",
            cases = emptyList(), resultDigest = "",
        ) }

        val report = evaluator.evaluate(request(cases = 1, mutants = listOf(DefectKind.OFF_BY_ONE)))

        // 0% 가 아니라 실패다. 콘텐츠가 깨진 것을 사용자 점수로 갚게 하면 안 된다.
        assertEquals(MutationStatus.FAILED, report.status)
        assertTrue(report.mutants.isEmpty())
    }

    @Test
    fun `대조할 오답이 없으면 0퍼센트가 아니라 잴 것이 없다`() {
        val report = MutationEvaluator { error("돌면 안 된다") }
            .evaluate(request(cases = 1, mutants = emptyList()))

        assertEquals(MutationStatus.NO_CASES, report.status)
        assertEquals(null, report.score())
    }

    @Test
    fun `잡은 케이스가 여럿이면 전부 돌려준다`() {
        val evaluator = MutationEvaluator { request ->
            if (request.source == REFERENCE) result(Verdict.ACCEPTED, Verdict.ACCEPTED, Verdict.ACCEPTED)
            else result(Verdict.WRONG_ANSWER, Verdict.ACCEPTED, Verdict.TIME_LIMIT)
        }

        val report = evaluator.evaluate(request(cases = 3, mutants = listOf(DefectKind.WRONG_BRANCH)))

        // 하나만 보이면 나머지를 지워도 된다고 읽힌다.
        assertEquals(listOf(1, 3), report.mutants.single().killedBy)
        assertFalse(report.mistakenCases.isNotEmpty())
    }

    private fun result(vararg verdicts: Verdict) = ExecutionResult(
        executionId = "x",
        submissionId = "x",
        attempt = 1,
        fencingToken = dev.codedrill.judge.protocol.FencingToken(1),
        terminalVerdict = null,
        compileLog = null,
        cases = verdicts.mapIndexed { index, verdict ->
            TestCaseResult("case-${index + 1}", "mutation", verdict, Measurements.NONE)
        },
        resultDigest = "",
    )

    private fun request(cases: Int, mutants: List<DefectKind>) = MutationRequest(
        evaluationId = "eval",
        correlationId = "eval",
        problemVersionId = "p@1",
        packageDigest = "d",
        signature = Signature("f", listOf(Parameter("n", ValueType.INT)), ValueType.INT),
        limits = Limits(timeMillis = 1000, memoryMb = 256, outputBytes = 65536),
        reference = REFERENCE,
        mutants = mutants.mapIndexed { index, kind ->
            MutantSource("m$index", kind, "mutant-$index")
        },
        cases = (1..cases).map { TestCase("case-$it", "mutation", listOf(it), it) },
    )

    private companion object {
        const val REFERENCE = "reference-source"
    }
}
