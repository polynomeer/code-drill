package dev.codedrill.judge.runner.execution

import dev.codedrill.judge.protocol.ExecutionMode
import dev.codedrill.judge.protocol.ExecutionRequest
import dev.codedrill.judge.protocol.ExecutionResult
import dev.codedrill.judge.protocol.FencingToken
import dev.codedrill.judge.protocol.Language
import dev.codedrill.judge.protocol.MutantOutcome
import dev.codedrill.judge.protocol.MutationReport
import dev.codedrill.judge.protocol.MutationRequest
import dev.codedrill.judge.protocol.MutationStatus
import dev.codedrill.judge.protocol.RequestedGroup
import dev.codedrill.judge.protocol.Verdict
import dev.codedrill.platform.problempackage.Aggregation
import dev.codedrill.platform.problempackage.GroupPolicy
import dev.codedrill.platform.problempackage.MutantSource
import dev.codedrill.platform.problempackage.StopPolicy
import dev.codedrill.platform.problempackage.TestCase
import dev.codedrill.platform.problempackage.Visibility

/**
 * 사용자 테스트를 대표 오답에 겨눈다 (PRD FR-804).
 *
 * 새로 지은 채점기가 아니다. **콘텐츠 검증이 하던 일의 방향을 뒤집었을 뿐이다** —
 * 저기서는 테스트를 고정해 두고 오답이 잡히는지 보았고, 여기서는 오답을 고정해 두고
 * 테스트가 잡는지 본다. 엔진은 같은 것을 쓴다. 다른 엔진을 쓰면 "저작 검증은 통과했는데
 * 사용자 화면에서는 안 잡힌다" 같은 것이 생긴다.
 *
 * 두 단계다.
 *
 * 1. **정답으로 사용자의 기대를 검산한다.** 기대 출력이 실제 정답과 다르면 그 케이스는
 *    시험이 아니라 틀린 시험이다. 그것으로 오답을 "잡았다"고 세면 점수가 거짓이 된다 —
 *    틀린 기대는 정답도 오답도 똑같이 떨어뜨리기 때문이다.
 * 2. **검산을 통과한 케이스로만** 오답을 돌린다. 하나라도 어긋나면 그 오답은 잡힌 것이다.
 */
class MutationEvaluator(
    /**
     * 실행 한 번.
     *
     * 엔진을 통째로 받지 않는다. 여기 있는 코드가 정하는 것은 **무엇을 몇 번 돌릴지와
     * 그 결과를 어떻게 읽을지**뿐이고, 그 판단은 샌드박스 없이도 시험할 수 있어야 한다.
     */
    private val execute: (ExecutionRequest) -> ExecutionResult,
) {

    constructor(engine: ExecutionEngine) : this(engine::execute)

    fun evaluate(request: MutationRequest): MutationReport {
        if (request.cases.isEmpty()) return report(request, MutationStatus.NO_CASES)
        if (request.mutants.isEmpty()) {
            // 저작 게이트가 빈 mutants/ 를 막지만(§6.3), 막히기 전의 문제가 이미 공개돼
            // 있을 수 있다. 그때 낼 수 있는 정직한 답은 0% 가 아니라 "잴 것이 없다"다.
            return report(request, MutationStatus.NO_CASES, "이 문제에는 대조할 오답이 없다")
        }

        val truth = run(request, request.reference, request.cases)
        if (truth.cases.isEmpty()) {
            // 정답이 컴파일되지 않았거나 플랫폼이 넘어졌다. 사용자 잘못이 아니다.
            return report(request, MutationStatus.FAILED, "정답 코드가 돌지 않았다")
        }

        // 정답이 떨어뜨린 케이스 = 사용자의 기대가 틀린 케이스. 정답 값은 돌려주지 않는다.
        val mistaken = truth.cases.withIndex()
            .filter { (_, case) -> case.verdict != Verdict.ACCEPTED }
            .map { (index, _) -> index }
        val valid = request.cases.filterIndexed { index, _ -> index !in mistaken }

        val outcomes = if (valid.isEmpty()) {
            // 오답을 돌릴 이유가 없다. 어차피 전부 "잡혔다"로 나오는데, 그 잡음의 원인은
            // 오답이 아니라 사용자의 틀린 기대다.
            emptyList()
        } else {
            request.mutants.map { mutant -> outcomeOf(request, mutant, valid, mistaken) }
        }

        return MutationReport(
            evaluationId = request.evaluationId,
            status = MutationStatus.COMPLETED,
            mistakenCases = mistaken.map { it + 1 },
            mutants = outcomes,
        )
    }

    /**
     * 오답 하나를 검산된 케이스에 돌린다.
     *
     * 컴파일에 실패하면(`cases` 가 비면) **잡히지 않은 것으로 센다.** 사용자의 테스트가
     * 잡은 것이 아니기 때문이다. 오답이 컴파일되지 않는 것은 콘텐츠 결함이고, 그것을
     * 사용자의 점수로 갚게 하면 안 된다.
     */
    private fun outcomeOf(
        request: MutationRequest,
        mutant: MutantSource,
        valid: List<TestCase>,
        mistaken: List<Int>,
    ): MutantOutcome {
        val result = run(request, mutant.source, valid)

        // 검산을 통과한 케이스만 돌렸으므로 결과의 i 번째는 valid 의 i 번째다. 사용자에게
        // 돌려줄 번호는 **원래 목록에서의 번호**여야 하므로 빠진 자리를 되메운다.
        val originalIndex = (request.cases.indices - mistaken.toSet()).toList()

        val killedBy = result.cases.withIndex()
            .filter { (_, case) -> case.verdict != Verdict.ACCEPTED }
            .mapNotNull { (index, _) -> originalIndex.getOrNull(index)?.plus(1) }

        return MutantOutcome(kind = mutant.kind, killed = killedBy.isNotEmpty(), killedBy = killedBy)
    }

    /**
     * 케이스를 **끝까지** 돌린다.
     *
     * 첫 실패에서 멈추면 "몇 번 케이스가 이 결함을 잡았나"를 하나밖에 말할 수 없다.
     * 잡은 케이스가 여럿이라는 것은 사용자가 알아야 할 사실이다 — 하나만 보이면 그
     * 케이스만 남기고 나머지를 지워도 된다고 읽힌다.
     */
    private fun run(request: MutationRequest, source: String, cases: List<TestCase>): ExecutionResult =
        execute(
            ExecutionRequest(
                executionId = "mutation-${request.evaluationId}",
                submissionId = "mutation-${request.evaluationId}",
                attempt = 1,
                fencingToken = FencingToken(1),
                correlationId = request.correlationId,
                problemVersionId = request.problemVersionId,
                packageDigest = request.packageDigest,
                // 도는 것은 저작자의 Kotlin 코드다. 사용자가 무엇으로 풀었든 상관없다.
                language = Language.KOTLIN,
                source = source,
                signature = request.signature,
                limits = request.limits,
                groups = listOf(RequestedGroup(GROUP, cases.map { it.copy(groupId = GROUP.id) })),
                // 실제 출력을 돌려받지 않는다. 정답의 출력이 봉투에 실리면 그것이 곧
                // 사용자가 원하는 답이고, 임의 입력에 대한 신탁이 된다 (§8.3).
                mode = ExecutionMode.JUDGE,
            ),
        )

    private fun report(request: MutationRequest, status: MutationStatus, message: String? = null) =
        MutationReport(evaluationId = request.evaluationId, status = status, message = message)

    private companion object {
        /** 사용자가 적은 입력이라 숨길 것이 없다. 끝까지 돌려야 하므로 CONTINUE 다. */
        val GROUP = GroupPolicy(
            id = "mutation",
            weight = 100,
            visibility = Visibility.PUBLIC,
            aggregation = Aggregation.SUM,
            stopPolicy = StopPolicy.CONTINUE,
        )
    }
}
