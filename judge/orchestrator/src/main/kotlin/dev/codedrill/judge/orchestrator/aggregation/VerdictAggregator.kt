package dev.codedrill.judge.orchestrator.aggregation

import dev.codedrill.judge.protocol.ExecutionResult
import dev.codedrill.judge.protocol.TestCaseResult
import dev.codedrill.judge.protocol.Verdict
import dev.codedrill.platform.problempackage.Aggregation
import dev.codedrill.platform.problempackage.GroupPolicy

/**
 * 실행 결과를 최종 판정으로 집계한다 (기술 설계서 §4.1 7단계, §6.2).
 *
 * **결정적이어야 한다.** 같은 결과 봉투를 몇 번을 집계하든 같은 값이 나와야 재채점과
 * 재현성 SLO(§12.1 Judge reproducibility)를 지킬 수 있다. 그래서 케이스 순회 순서는
 * 그룹 정의 순서와 케이스 id 정렬 순서로 고정하고, 집합 자료구조의 순회에 기대지 않는다.
 */
object VerdictAggregator {

    fun aggregate(policies: List<GroupPolicy>, result: ExecutionResult): AggregatedResult {
        // 컴파일 실패나 플랫폼 장애처럼 케이스 실행 전에 끝난 경우.
        result.terminalVerdict?.let { terminal ->
            return AggregatedResult(
                verdict = terminal,
                score = 0,
                groups = emptyList(),
                compileLog = result.compileLog,
            )
        }

        val byGroup = result.cases.groupBy { it.groupId }
        val groups = policies.map { policy ->
            val cases = byGroup[policy.id].orEmpty().sortedBy { it.caseId }
            GroupResult(
                groupId = policy.id,
                verdict = groupVerdict(cases),
                score = score(policy, cases),
                maxScore = policy.weight,
                cases = cases,
            )
        }

        return AggregatedResult(
            verdict = overallVerdict(groups),
            score = groups.sumOf { it.score },
            groups = groups,
            compileLog = null,
        )
    }

    /**
     * 그룹 판정은 첫 실패 케이스의 판정이다. 실패가 없으면 ACCEPTED.
     *
     * 케이스가 하나도 없는 그룹은 실행되지 못한 것이므로 SYSTEM_ERROR 다. 채점하지 못한
     * 것을 통과로 처리하면 판정이 조용히 헐거워진다.
     */
    private fun groupVerdict(cases: List<TestCaseResult>): Verdict {
        if (cases.isEmpty()) return Verdict.SYSTEM_ERROR
        return cases.firstOrNull { it.verdict != Verdict.ACCEPTED }?.verdict ?: Verdict.ACCEPTED
    }

    private fun score(policy: GroupPolicy, cases: List<TestCaseResult>): Int {
        if (cases.isEmpty()) return 0
        val passed = cases.count { it.verdict == Verdict.ACCEPTED }
        return when (policy.aggregation) {
            Aggregation.ALL_OR_NOTHING -> if (passed == cases.size) policy.weight else 0
            // 내림으로 계산해 부분 점수가 만점을 넘지 않게 한다.
            Aggregation.SUM -> policy.weight * passed / cases.size
        }
    }

    /**
     * 최종 판정.
     *
     * SYSTEM_ERROR 는 사용자 실패와 다른 축이므로 언제나 먼저 본다. 플랫폼 장애를
     * 오답으로 덮으면 사용자가 자기 코드를 고치려 들게 된다 (§4.4).
     */
    private fun overallVerdict(groups: List<GroupResult>): Verdict = when {
        groups.any { it.verdict == Verdict.SYSTEM_ERROR } -> Verdict.SYSTEM_ERROR
        groups.all { it.verdict == Verdict.ACCEPTED } -> Verdict.ACCEPTED
        else -> groups.first { it.verdict != Verdict.ACCEPTED }.verdict
    }
}

data class AggregatedResult(
    val verdict: Verdict,
    val score: Int,
    val groups: List<GroupResult>,
    val compileLog: String?,
)

data class GroupResult(
    val groupId: String,
    val verdict: Verdict,
    val score: Int,
    val maxScore: Int,
    val cases: List<TestCaseResult>,
)
