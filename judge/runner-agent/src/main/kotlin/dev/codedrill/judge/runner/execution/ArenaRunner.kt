package dev.codedrill.judge.runner.execution

import dev.codedrill.judge.protocol.ArenaReport
import dev.codedrill.judge.protocol.ArenaRequest
import dev.codedrill.judge.protocol.ArenaResult
import dev.codedrill.judge.protocol.ArenaStatus
import dev.codedrill.judge.protocol.ExecutionMode
import dev.codedrill.judge.protocol.ExecutionRequest
import dev.codedrill.judge.protocol.ExecutionResult
import dev.codedrill.judge.protocol.FencingToken
import dev.codedrill.judge.protocol.Language
import dev.codedrill.judge.protocol.RequestedGroup
import dev.codedrill.judge.protocol.ShrinkRequest
import dev.codedrill.judge.protocol.ShrinkStatus
import dev.codedrill.judge.protocol.Verdict
import dev.codedrill.platform.problempackage.Aggregation
import dev.codedrill.platform.problempackage.GroupPolicy
import dev.codedrill.platform.problempackage.StopPolicy
import dev.codedrill.platform.problempackage.TestCase
import dev.codedrill.platform.problempackage.Visibility

/**
 * 반례 아레나 (§8.3).
 *
 * 1. 참조 풀이로 입력을 검산한다. 참조가 터지면 문제의 전제를 깨뜨린 입력이다 — 그것으로
 *    오답을 "깨뜨렸다"고 하면 안 된다. 전제 밖에서는 정답도 오답도 없다.
 * 2. 오답마다 돌려 정답과 견준다.
 * 3. 깨뜨린 오답마다 축소기를 돌린다. 사용자가 적은 입력에서 시작하므로 빠르다.
 */
class ArenaRunner(
    private val execute: (ExecutionRequest) -> ExecutionResult,
    private val shrinker: Shrinker,
) {

    constructor(engine: ExecutionEngine) : this(engine::execute, Shrinker(engine))

    fun run(request: ArenaRequest): ArenaReport {
        val truth = execute(requestFor(request, request.reference)).cases.firstOrNull()
        val expected = truth?.actual
        if (truth == null || truth.verdict != Verdict.ACCEPTED || expected == null) {
            return ArenaReport(
                attemptId = request.attemptId,
                status = ArenaStatus.INVALID_INPUT,
                message = "참조 풀이가 이 입력을 다루지 못했다. 문제의 전제를 확인하세요",
            )
        }

        val results = request.mutants.map { mutant ->
            val ours = execute(requestFor(request, mutant.source)).cases.firstOrNull()
            val broken = ours == null || ours.verdict != Verdict.ACCEPTED || ours.actual != expected
            val minimal = if (broken) shrink(request, mutant.source) else null
            ArenaResult(
                name = mutant.name,
                kind = mutant.kind,
                broken = broken,
                actual = ours?.actual ?: ours?.verdict?.name,
                minimalArgs = minimal?.args,
                minimalSize = minimal?.minimalSize,
            )
        }
        return ArenaReport(attemptId = request.attemptId, status = ArenaStatus.COMPLETED, results = results)
    }

    private fun shrink(request: ArenaRequest, mutant: String) = shrinker.shrink(
        ShrinkRequest(
            shrinkId = request.attemptId,
            correlationId = request.correlationId,
            problemVersionId = request.problemVersionId,
            packageDigest = request.packageDigest,
            signature = request.signature,
            limits = request.limits,
            reference = request.reference,
            language = Language.KOTLIN,
            source = mutant,
            starts = listOf(request.args),
        ),
    ).takeIf { it.status == ShrinkStatus.FOUND }

    private fun requestFor(request: ArenaRequest, source: String) = ExecutionRequest(
        executionId = "arena-${request.attemptId}",
        submissionId = "arena-${request.attemptId}",
        attempt = 1,
        fencingToken = FencingToken(1),
        correlationId = request.correlationId,
        problemVersionId = request.problemVersionId,
        packageDigest = request.packageDigest,
        language = Language.KOTLIN,
        source = source,
        signature = request.signature,
        limits = request.limits,
        groups = listOf(RequestedGroup(GROUP, listOf(TestCase("arena", GROUP.id, request.args, expected = null)))),
        mode = ExecutionMode.TRIAL,
    )

    private companion object {
        val GROUP = GroupPolicy("arena", 100, Visibility.PUBLIC, Aggregation.SUM, StopPolicy.CONTINUE)
    }
}
