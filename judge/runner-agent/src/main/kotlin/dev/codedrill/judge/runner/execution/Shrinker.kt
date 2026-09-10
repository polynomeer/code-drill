package dev.codedrill.judge.runner.execution

import dev.codedrill.judge.protocol.ExecutionMode
import dev.codedrill.judge.protocol.ExecutionRequest
import dev.codedrill.judge.protocol.ExecutionResult
import dev.codedrill.judge.protocol.FencingToken
import dev.codedrill.judge.protocol.Language
import dev.codedrill.judge.protocol.RequestedGroup
import dev.codedrill.judge.protocol.ShrinkReport
import dev.codedrill.judge.protocol.ShrinkRequest
import dev.codedrill.judge.protocol.ShrinkStatus
import dev.codedrill.judge.protocol.TestCaseResult
import dev.codedrill.judge.protocol.Verdict
import dev.codedrill.platform.problempackage.Aggregation
import dev.codedrill.platform.problempackage.GroupPolicy
import dev.codedrill.platform.problempackage.StopPolicy
import dev.codedrill.platform.problempackage.TestCase
import dev.codedrill.platform.problempackage.Visibility

/**
 * 최소 반례 축소 (§6.3, PRD §3.4 검증군 증거).
 *
 * delta debugging 이다. 입력을 잘라 보고, 잘라도 여전히 틀리면 그 작은 쪽을 취한다.
 *
 * **후보가 유효한 입력인지는 참조 풀이가 판단한다.** 원소를 지우다 보면 "정답은 항상
 * 정확히 하나 존재한다" 같은 문제의 전제를 깨뜨린 입력이 나오는데, 그런 입력에서는
 * 참조 풀이도 터진다. 그것을 떨어뜨리면 문제별 shrinker 없이도 유효한 입력만 남는다 —
 * 무엇이 유효한 입력인지 이미 아는 코드가 저장소에 있는데 그 지식을 다시 쓸 이유가 없다.
 *
 * 한 라운드가 실행 두 번이다. 참조를 먼저 돌려 **정답과 유효성**을 얻고, 같은 후보에
 * 사용자 코드를 돌려 견준다. 둘을 한 번에 돌릴 수는 없다 — 언어가 다를 수 있다.
 */
class Shrinker(private val execute: (ExecutionRequest) -> ExecutionResult) {

    constructor(engine: ExecutionEngine) : this(engine::execute)

    fun shrink(request: ShrinkRequest): ShrinkReport {
        if (request.starts.isEmpty()) return failed(request, "시작할 입력이 없다")

        // 어느 케이스가 떨어졌는지는 제어 영역이 모른다 (§8.3). 후보를 전부 돌려 보고
        // **재현되는 가장 작은 것**에서 시작한다 — 그러면 줄일 것이 이미 적다.
        val evaluated = evaluate(request, request.starts)
        evaluated.filterIsInstance<Candidate.Broken>().firstOrNull()?.let {
            return failed(request, it.reason)
        }

        val startOutcome = evaluated.filterIsInstance<Candidate.Counterexample>()
            .minByOrNull { sizeOf(it.args) }
            ?: return ShrinkReport(
                shrinkId = request.shrinkId,
                status = ShrinkStatus.NOT_REPRODUCED,
                message = "받은 입력 어디에서도 참조 풀이와 다른 답이 나오지 않았다",
                originalSize = request.starts.minOfOrNull { sizeOf(it) } ?: 0,
            )

        val start = startOutcome.args
        var best: Candidate.Counterexample = startOutcome
        var rounds = 0

        while (rounds < ShrinkRequest.MAX_ROUNDS) {
            val candidates = reductionsOf(best.args).take(ShrinkRequest.CANDIDATES_PER_ROUND)
            if (candidates.isEmpty()) break

            rounds += 1
            val smaller = evaluate(request, candidates)
                .filterIsInstance<Candidate.Counterexample>()
                // 가장 작은 것을 취한다. 같은 크기가 여럿이면 앞의 것 — 후보 생성이
                // 결정적이므로, 같은 제출은 늘 같은 반례를 낸다.
                .minByOrNull { sizeOf(it.args) }
                ?: break

            if (sizeOf(smaller.args) >= sizeOf(best.args)) break
            best = smaller
        }

        return ShrinkReport(
            shrinkId = request.shrinkId,
            status = ShrinkStatus.FOUND,
            args = best.args,
            originalSize = sizeOf(start),
            minimalSize = sizeOf(best.args),
            rounds = rounds,
            actual = best.actual,
            expected = best.expected,
        )
    }

    /**
     * 후보들을 한 라운드에 시험한다.
     *
     * 참조를 먼저 돌린다. 참조가 터지거나 답을 못 내놓은 후보는 **입력 자체가 틀린 것**이라
     * 여기서 떨어진다 — 그 위에서 사용자 코드를 견줘 봐야 아무 뜻이 없다.
     */
    private fun evaluate(request: ShrinkRequest, candidates: List<List<Any>>): List<Candidate> {
        val reference = run(request, Language.KOTLIN, request.reference, candidates)
        val mine = run(request, request.language, request.source, candidates)

        if (reference.cases.isEmpty()) {
            return listOf(Candidate.Broken("참조 풀이가 돌지 않았다"))
        }
        if (mine.cases.isEmpty()) {
            return listOf(Candidate.Broken(mine.compileLog?.let { "제출이 컴파일되지 않았다" } ?: "제출이 돌지 않았다"))
        }

        val byId = { result: ExecutionResult -> result.cases.associateBy { it.caseId } }
        val referenceById = byId(reference)
        val mineById = byId(mine)

        return candidates.mapIndexedNotNull { index, args ->
            val id = caseId(index)
            val truth = referenceById[id] ?: return@mapIndexedNotNull null
            val ours = mineById[id] ?: return@mapIndexedNotNull null

            // 참조가 이 입력을 다루지 못하면 유효한 입력이 아니다.
            val expected = truth.actual
            if (truth.verdict != Verdict.ACCEPTED || expected == null) return@mapIndexedNotNull null

            if (isCounterexample(ours, truth)) {
                Candidate.Counterexample(args, actual = describe(ours), expected = expected)
            } else {
                null
            }
        }
    }

    /**
     * 반례인가.
     *
     * 값이 다른 것만이 아니다 — 사용자 코드가 터지거나 한도를 넘긴 것도 참조가 멀쩡히
     * 끝낸 입력에서라면 반례다.
     */
    private fun isCounterexample(ours: TestCaseResult, truth: TestCaseResult): Boolean =
        ours.verdict != Verdict.ACCEPTED || ours.actual != truth.actual

    private fun describe(result: TestCaseResult): String =
        result.actual ?: result.verdict.name

    /**
     * 한 단계 더 작은 후보들.
     *
     * 배열 인자마다 **연속 구간 하나를 지운** 것들을 만든다. 구간 길이를 절반씩 줄여
     * 가며 크게도 작게도 시도한다 — 큰 덩어리가 한 번에 떨어지면 라운드가 크게 절약되고,
     * 작은 조각은 마지막 한두 원소를 떼는 데 쓰인다.
     *
     * 정수 인자는 건드리지 않는다. 배열과 짝이 맞아야 하는 값(두 수의 합의 `target` 같은)을
     * 임의로 줄이면 전제가 깨진 입력만 잔뜩 나오고, 그것들은 참조가 전부 떨어뜨린다.
     */
    private fun reductionsOf(args: List<Any>): List<List<Any>> = buildList {
        args.forEachIndexed { index, value ->
            val list = value as? List<*> ?: return@forEachIndexed
            if (list.size <= 1) return@forEachIndexed

            var span = list.size / 2
            while (span >= 1) {
                var from = 0
                while (from < list.size) {
                    val to = minOf(from + span, list.size)
                    add(args.replacing(index, list.withoutRange(from, to)))
                    from += span
                }
                span /= 2
            }
        }
    }

    private fun List<Any>.replacing(index: Int, value: Any): List<Any> =
        toMutableList().also { it[index] = value }

    private fun List<*>.withoutRange(from: Int, to: Int): List<Any?> =
        filterIndexed { index, _ -> index < from || index >= to }

    /** 크기 = 배열 원소 수의 합. 정수 인자는 세지 않는다 — 줄이지 않기 때문이다. */
    private fun sizeOf(args: List<Any>): Int =
        args.sumOf { (it as? List<*>)?.size ?: 0 }

    private fun run(
        request: ShrinkRequest,
        language: Language,
        source: String,
        candidates: List<List<Any>>,
    ): ExecutionResult = execute(
        ExecutionRequest(
            executionId = "shrink-${request.shrinkId}",
            submissionId = "shrink-${request.shrinkId}",
            attempt = 1,
            fencingToken = FencingToken(1),
            correlationId = request.correlationId,
            problemVersionId = request.problemVersionId,
            packageDigest = request.packageDigest,
            language = language,
            source = source,
            signature = request.signature,
            limits = request.limits,
            groups = listOf(
                RequestedGroup(
                    GROUP,
                    candidates.mapIndexed { index, args ->
                        // 기대를 적지 않는다. 여기서 알아야 할 것은 통과 여부가 아니라
                        // **무엇이 나왔나**이고, 그 값은 TRIAL 모드에서만 실려 온다.
                        TestCase(caseId(index), GROUP.id, args, expected = null)
                    },
                ),
            ),
            mode = ExecutionMode.TRIAL,
        ),
    )

    private fun caseId(index: Int) = "c${index + 1}"

    private fun failed(request: ShrinkRequest, reason: String) = ShrinkReport(
        shrinkId = request.shrinkId,
        status = ShrinkStatus.FAILED,
        message = reason,
        originalSize = request.starts.minOfOrNull { sizeOf(it) } ?: 0,
    )

    private sealed interface Candidate {
        data class Counterexample(
            val args: List<Any>,
            val actual: String,
            val expected: String,
        ) : Candidate

        data class Broken(val reason: String) : Candidate
    }

    private companion object {
        /** 축소 후보는 전부 끝까지 돌린다. 첫 실패에서 멈추면 나머지 후보를 못 본다. */
        val GROUP = GroupPolicy(
            id = "shrink",
            weight = 100,
            visibility = Visibility.PUBLIC,
            aggregation = Aggregation.SUM,
            stopPolicy = StopPolicy.CONTINUE,
        )
    }
}
