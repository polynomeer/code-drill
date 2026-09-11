package dev.codedrill.judge.runner.execution

import dev.codedrill.judge.protocol.Approach
import dev.codedrill.judge.protocol.ApproachResult
import dev.codedrill.judge.protocol.ExecutionMode
import dev.codedrill.judge.protocol.ExecutionRequest
import dev.codedrill.judge.protocol.ExecutionResult
import dev.codedrill.judge.protocol.FencingToken
import dev.codedrill.judge.protocol.LabReport
import dev.codedrill.judge.protocol.LabRequest
import dev.codedrill.judge.protocol.Measurements
import dev.codedrill.judge.protocol.RequestedGroup
import dev.codedrill.judge.protocol.TraceManifest
import dev.codedrill.judge.protocol.Verdict
import dev.codedrill.platform.problempackage.Aggregation
import dev.codedrill.platform.problempackage.GroupPolicy
import dev.codedrill.platform.problempackage.StopPolicy
import dev.codedrill.platform.problempackage.TestCase
import dev.codedrill.platform.problempackage.Visibility

/**
 * 실험실 (기획서 §6.4~6.6).
 *
 * 풀이마다 같은 입력으로 계측 실행을 한 번씩 한다. 한 샌드박스에서 전부 돌리지 않는
 * 이유는 측정 때문이다 — 같은 머신에서 동시에 돌면 서로의 시간을 오염시킨다 (§5.2).
 *
 * 컴파일 실패나 시스템 오류는 결과에 그대로 남긴다. 풀이 하나가 안 돌았다고 나머지를
 * 버리면 사용자는 어느 것이 안 돌았는지도 모른다.
 */
class LabRunner(private val execute: (ExecutionRequest) -> ExecutionResult) {

    constructor(engine: ExecutionEngine) : this(engine::execute)

    fun run(request: LabRequest): LabReport = LabReport(
        labId = request.labId,
        results = request.approaches.take(LabRequest.MAX_APPROACHES).map { approach ->
            toResult(approach, execute(requestFor(request, approach)))
        },
    )

    private fun toResult(approach: Approach, result: ExecutionResult): ApproachResult {
        val case = result.cases.firstOrNull()
        val events = result.trace?.events.orEmpty()
        return ApproachResult(
            label = approach.label,
            verdict = result.terminalVerdict ?: case?.verdict ?: Verdict.SYSTEM_ERROR,
            actual = case?.actual,
            measurements = case?.measurements ?: Measurements.NONE,
            eventCounts = events.groupingBy { it.eventType }.eachCount(),
            events = events.take(TraceManifest.EVENT_BUDGET),
            truncated = result.trace?.truncated ?: false,
        )
    }

    private fun requestFor(request: LabRequest, approach: Approach) = ExecutionRequest(
        executionId = "lab-${request.labId}-${approach.label}",
        submissionId = "lab-${request.labId}",
        attempt = 1,
        fencingToken = FencingToken(1),
        correlationId = request.correlationId,
        problemVersionId = request.problemVersionId,
        packageDigest = request.packageDigest,
        language = approach.language,
        source = approach.source,
        signature = request.signature,
        limits = request.limits,
        // 기대를 적지 않는다. 실험실은 채점이 아니라 관찰이고, 무엇이 나왔는지가 답이다.
        groups = listOf(RequestedGroup(GROUP, listOf(TestCase("lab", GROUP.id, request.args, expected = null)))),
        mode = ExecutionMode.LAB,
    )

    private companion object {
        val GROUP = GroupPolicy(
            id = "lab",
            weight = 100,
            visibility = Visibility.PUBLIC,
            aggregation = Aggregation.SUM,
            stopPolicy = StopPolicy.CONTINUE,
        )
    }
}
