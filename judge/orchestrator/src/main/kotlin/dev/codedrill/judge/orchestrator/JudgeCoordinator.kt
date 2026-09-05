package dev.codedrill.judge.orchestrator

import dev.codedrill.judge.orchestrator.aggregation.VerdictAggregator
import dev.codedrill.judge.orchestrator.lease.AttemptRegistry
import dev.codedrill.judge.protocol.CompletedGroup
import dev.codedrill.judge.protocol.ExecutionMode
import dev.codedrill.judge.protocol.ExecutionRequest
import dev.codedrill.judge.protocol.ExecutionResult
import dev.codedrill.judge.protocol.JudgeCompleted
import dev.codedrill.judge.protocol.JudgeProgressed
import dev.codedrill.judge.protocol.JudgeStatus
import dev.codedrill.judge.protocol.RequestedGroup
import dev.codedrill.judge.protocol.SubmissionQueued
import dev.codedrill.judge.protocol.TraceReady
import dev.codedrill.platform.observability.CorrelationIds
import dev.codedrill.platform.problempackage.ProblemPackage
import dev.codedrill.platform.problempackage.ProblemPackageLoader
import dev.codedrill.platform.problempackage.Visibility
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * 채점 흐름의 조정자 (기술 설계서 §4.1).
 *
 * 전달 경로(AMQP)와 분리해 두어 브로커 없이도 불변식을 검증할 수 있다. 실제 배선은
 * [dev.codedrill.judge.orchestrator.messaging] 에서 이 클래스를 감싼다.
 */
class JudgeCoordinator(
    private val packages: ProblemPackageLoader,
    private val registry: AttemptRegistry,
    private val gateway: JudgeGateway,
) {

    private val log = LoggerFactory.getLogger(javaClass)
    private val progressSeq = AtomicLong()

    /**
     * 판정이 끝난 뒤 트레이스를 이어 만들기 위해 원 요청을 잠시 들고 있는다.
     *
     * 슬라이스는 메모리에 둔다. 오케스트레이터가 재시작하면 대기 중이던 트레이스 요청은
     * 사라지지만, 판정은 이미 끝났고 트레이스는 재요청할 수 있으므로 사용자에게 남는
     * 피해가 없다 (§12.2 장애 격리).
     */
    private val pendingTrace = ConcurrentHashMap<String, ExecutionRequest>()

    /** 제출을 임대하고 Runner 에게 실행을 요청한다. */
    fun onSubmissionQueued(message: SubmissionQueued) {
        val pkg = packages.load(message.problemId)
        require(pkg.manifest.version == message.problemVersion) {
            "요청한 문제 버전이 로드된 패키지와 다르다: ${message.problemVersion} != ${pkg.manifest.version}"
        }

        val lease = registry.lease(message.submissionId)
        val request = ExecutionRequest(
            executionId = UUID.randomUUID().toString(),
            submissionId = message.submissionId,
            attempt = lease.attempt,
            fencingToken = lease.token,
            correlationId = message.correlationId,
            problemVersionId = pkg.problemVersionId,
            packageDigest = pkg.packageDigest,
            language = message.language,
            source = message.source,
            signature = pkg.manifest.signature,
            limits = pkg.manifest.limits,
            groups = pkg.groups.map { RequestedGroup(it.policy, it.cases) },
        )

        log.atInfo()
            .addKeyValue(CorrelationIds.SUBMISSION_ID, message.submissionId)
            .addKeyValue(CorrelationIds.EXECUTION_ID, request.executionId)
            .addKeyValue(CorrelationIds.ATTEMPT, lease.attempt)
            .log("실행을 임대하고 Runner 에 요청한다")

        if (message.requestTrace) pendingTrace[message.submissionId] = request
        gateway.requestExecution(request)
        gateway.publishProgress(
            JudgeProgressed(
                submissionId = message.submissionId,
                correlationId = message.correlationId,
                seq = progressSeq.incrementAndGet(),
                status = JudgeStatus.LEASED,
            ),
        )
    }

    /**
     * Runner 결과를 검증하고 집계한다.
     *
     * 검증에 걸린 결과는 예외를 던지지 않고 조용히 버린다. 브로커가 재전달할 이유를
     * 만들지 않기 위해서다 — 스테일 결과는 다시 받아도 여전히 스테일이다.
     */
    fun onExecutionResult(result: ExecutionResult, correlationId: String) {
        // 트레이스는 판정이 아니다. 임대·fencing 검사를 거치지 않고, 실패해도 판정에
        // 영향을 주지 않는다 (§7.1, §12.2).
        if (result.mode == ExecutionMode.TRACE) {
            result.trace?.let { capture ->
                gateway.publishTraceReady(
                    TraceReady(
                        submissionId = result.submissionId,
                        executionId = result.executionId,
                        correlationId = correlationId,
                        capture = capture,
                    ),
                )
            }
            return
        }

        when (val acceptance = registry.accept(result)) {
            AttemptRegistry.Acceptance.Accepted -> complete(result, correlationId)

            AttemptRegistry.Acceptance.Duplicate ->
                log.atInfo()
                    .addKeyValue(CorrelationIds.SUBMISSION_ID, result.submissionId)
                    .log("같은 결과가 다시 도착했다. no-op")

            AttemptRegistry.Acceptance.AlreadyCompleted ->
                log.atWarn()
                    .addKeyValue(CorrelationIds.SUBMISSION_ID, result.submissionId)
                    .addKeyValue(CorrelationIds.EXECUTION_ID, result.executionId)
                    .log("종료된 제출에 다른 결과가 도착했다. 감사 대상")

            is AttemptRegistry.Acceptance.Stale ->
                log.atWarn()
                    .addKeyValue(CorrelationIds.SUBMISSION_ID, result.submissionId)
                    .addKeyValue(CorrelationIds.EXECUTION_ID, result.executionId)
                    .log("스테일 결과를 폐기했다: ${acceptance.reason}")
        }
    }

    private fun complete(result: ExecutionResult, correlationId: String) {
        dispatchTrace(result.submissionId)
        val pkg = packageOf(result)
        val aggregated = VerdictAggregator.aggregate(pkg.groups.map { it.policy }, result)

        gateway.publishCompleted(
            JudgeCompleted(
                submissionId = result.submissionId,
                executionId = result.executionId,
                correlationId = correlationId,
                verdict = aggregated.verdict,
                score = aggregated.score,
                compileLog = aggregated.compileLog,
                groups = aggregated.groups.map { group ->
                    val policy = pkg.group(group.groupId).policy
                    CompletedGroup(
                        groupId = group.groupId,
                        verdict = group.verdict,
                        score = group.score,
                        maxScore = group.maxScore,
                        // 숨은 그룹의 케이스 내역은 여기서 잘라낸다. 제어 영역이 그대로
                        // 내려보내도 숨은 테스트가 새지 않아야 한다 (§8.3, §9.1).
                        cases = if (policy.visibility == Visibility.PUBLIC) group.cases else emptyList(),
                    )
                },
            ),
        )
    }

    /**
     * 판정이 끝난 뒤 학습용 트레이스를 별도 작업으로 띄운다 (§7.1).
     *
     * 저우선순위 큐로 분리하는 것이 다음 단계다. 지금은 같은 큐를 쓰되, 계측 오버헤드가
     * 판정 실행에 섞이지 않는다는 본질은 모드 분리로 지켜진다.
     */
    private fun dispatchTrace(submissionId: String) {
        val original = pendingTrace.remove(submissionId) ?: return
        gateway.requestExecution(
            original.copy(
                executionId = UUID.randomUUID().toString(),
                mode = ExecutionMode.TRACE,
            ),
        )
    }

    /** 제출이 어떤 문제를 풀고 있었는지. 슬라이스는 문제가 하나뿐이라 단순하다. */
    private fun packageOf(result: ExecutionResult): ProblemPackage {
        check(result.submissionId.isNotBlank())
        return packages.load(SLICE_PROBLEM_ID)
    }

    private companion object {
        /**
         * 첫 vertical slice 는 문제 하나만 다룬다 (§16.2). 실행 결과 봉투에 문제 버전을
         * 실어 보내도록 넓히는 것이 다음 단계다.
         */
        const val SLICE_PROBLEM_ID = "two-sum"
    }
}

/** 조정자가 바깥으로 보내는 것들. 테스트에서는 가짜로 바꿔 끼운다. */
interface JudgeGateway {
    fun requestExecution(request: ExecutionRequest)
    fun publishProgress(progress: JudgeProgressed)
    fun publishCompleted(completed: JudgeCompleted)
    fun publishTraceReady(ready: TraceReady)
}
