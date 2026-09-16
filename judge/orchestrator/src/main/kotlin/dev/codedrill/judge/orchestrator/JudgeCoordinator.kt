package dev.codedrill.judge.orchestrator

import dev.codedrill.judge.orchestrator.aggregation.VerdictAggregator
import dev.codedrill.judge.orchestrator.bundle.BundlePublisher
import dev.codedrill.judge.orchestrator.lease.Acceptance
import dev.codedrill.judge.orchestrator.lease.Lease
import dev.codedrill.judge.orchestrator.lease.LeaseRegistry
import dev.codedrill.judge.protocol.CaseSelection
import dev.codedrill.judge.protocol.CompletedGroup
import dev.codedrill.judge.protocol.ExecutionMode
import dev.codedrill.judge.protocol.ExecutionRequest
import dev.codedrill.judge.protocol.ExecutionHeartbeat
import dev.codedrill.judge.protocol.ExecutionResult
import dev.codedrill.judge.protocol.JudgeCompleted
import dev.codedrill.judge.protocol.JudgeProgressed
import dev.codedrill.judge.protocol.JudgeStatus
import dev.codedrill.judge.protocol.ProjectQueued
import dev.codedrill.judge.protocol.SubmissionQueued
import dev.codedrill.judge.orchestrator.trace.TraceProcessor
import dev.codedrill.judge.protocol.TraceReady
import dev.codedrill.platform.observability.CorrelationIds
import dev.codedrill.platform.problempackage.ProblemPackage
import dev.codedrill.platform.problempackage.ProblemPackageLoader
import dev.codedrill.platform.problempackage.Visibility
import org.slf4j.LoggerFactory
import dev.codedrill.judge.protocol.Verdict
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

/**
 * 채점 흐름의 조정자 (기술 설계서 §4.1).
 *
 * 전달 경로(AMQP)와 분리해 두어 브로커 없이도 불변식을 검증할 수 있다. 실제 배선은
 * [dev.codedrill.judge.orchestrator.messaging] 에서 이 클래스를 감싼다.
 */
class JudgeCoordinator(
    private val packages: ProblemPackageLoader,
    private val registry: LeaseRegistry,
    private val gateway: JudgeGateway,
    private val bundles: BundlePublisher,
    private val metrics: JudgeMetrics = JudgeMetrics(),
    /**
     * 같은 제출을 몇 번까지 다시 실행할지.
     *
     * 무한 재시도는 장애를 감추기만 한다 — 사용자는 영영 끝나지 않는 채점을 보고,
     * 대시보드에는 아무 이상도 뜨지 않는다. 한계를 넘으면 SYSTEM_ERROR 로 끝내
     * 사용자에게도 경보에도 드러나게 한다 (§4.4).
     */
    private val maxAttempts: Int = 3,
    /**
     * 두 번째 판정기 (11단계). 임대 표는 하나라 만료 회수도 여기서 한 번에 훑고, 원 요청이
     * 프로젝트형이면 저쪽에 넘긴다. null 이면 프로젝트형 임대는 회수하지 않고 포기한다 —
     * 프로젝트 판정기가 없는 조립(테스트)에서만 그렇다.
     */
    private val projects: ProjectCoordinator? = null,
) {

    private val log = LoggerFactory.getLogger(javaClass)
    private val progressSeq = AtomicLong()

    /**
     * 제출을 임대하고 Runner 에게 실행을 요청한다.
     *
     * 조정자는 아무것도 들고 있지 않는다. 다시 걸 때 필요한 원 요청과 판정 뒤 트레이스를
     * 이을지의 여부는 임대 안에 있다 — 그래야 재시작한 인스턴스도, 다른 인스턴스도 같은
     * 것을 본다 (§4.3).
     */
    fun onSubmissionQueued(message: SubmissionQueued) {
        val pkg = load(message)
        val lease = registry.lease(message, executionId = UUID.randomUUID().toString())
        val request = request(lease, pkg)

        log.atInfo()
            .addKeyValue(CorrelationIds.SUBMISSION_ID, message.submissionId)
            .addKeyValue(CorrelationIds.EXECUTION_ID, request.executionId)
            .addKeyValue(CorrelationIds.ATTEMPT, lease.attempt)
            .log("실행을 임대하고 Runner 에 요청한다")

        message.queuedAt?.let { metrics.queueWait(Duration.between(it, Instant.now())) }

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
                // 가공은 판정 경로 밖에서 한다 (§7.3). 실패해도 판정에 영향을 주지 않도록
                // 예외를 밖으로 흘리지 않는다 — 브로커가 재전달해도 같은 결과다.
                val startedAt = Instant.now()
                val processed = runCatching {
                    TraceProcessor.process(
                        traceId = result.executionId,
                        submissionId = result.submissionId,
                        capture = capture,
                    )
                }.getOrElse { error ->
                    metrics.traceInvalid(error::class.simpleName ?: "unknown")
                    log.warn("트레이스 가공에 실패했다: {}", error.message)
                    return
                }
                metrics.traceProcessed(
                    took = Duration.between(startedAt, Instant.now()),
                    captured = capture.events.size,
                    kept = processed.manifest.eventCount,
                )

                gateway.publishTraceReady(
                    TraceReady(
                        submissionId = result.submissionId,
                        executionId = result.executionId,
                        correlationId = correlationId,
                        manifest = processed.manifest,
                        chunks = processed.chunks,
                    ),
                )
            }
            return
        }

        val acceptance = registry.accept(result)
        metrics.acceptance(acceptance)

        when (acceptance) {
            is Acceptance.Accepted -> complete(result, correlationId, acceptance.origin as? SubmissionQueued)

            // 임대 기록이 없어도 결과는 넘긴다. 버리면 그 제출은 결과가 멀쩡히 도착했는데도
            // 영영 끝나지 않는다. 임대가 Redis 에 있으므로 이제 재시작만으로는 생기지 않는다 —
            // 보인다면 임대가 치워진 뒤 결과가 온 것이고, 트레이스는 이어 만들지 못한다.
            Acceptance.Unleased -> {
                log.atWarn()
                    .addKeyValue(CorrelationIds.SUBMISSION_ID, result.submissionId)
                    .addKeyValue(CorrelationIds.EXECUTION_ID, result.executionId)
                    .log("임대 기록이 없는 결과다. 넘긴다")
                complete(result, correlationId, origin = null)
            }

            Acceptance.Duplicate ->
                log.atInfo()
                    .addKeyValue(CorrelationIds.SUBMISSION_ID, result.submissionId)
                    .log("같은 결과가 다시 도착했다. no-op")

            Acceptance.AlreadyCompleted ->
                log.atWarn()
                    .addKeyValue(CorrelationIds.SUBMISSION_ID, result.submissionId)
                    .addKeyValue(CorrelationIds.EXECUTION_ID, result.executionId)
                    .log("종료된 제출에 다른 결과가 도착했다. 감사 대상")

            is Acceptance.Stale ->
                log.atWarn()
                    .addKeyValue(CorrelationIds.SUBMISSION_ID, result.submissionId)
                    .addKeyValue(CorrelationIds.EXECUTION_ID, result.executionId)
                    .log("스테일 결과를 폐기했다: ${acceptance.reason}")
        }
    }

    private fun complete(result: ExecutionResult, correlationId: String, origin: SubmissionQueued?) {
        val pkg = packageOf(result)
        if (origin != null && origin.requestTrace) dispatchTrace(result, origin, pkg)
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
     * Runner 가 실행을 붙들고 있다고 알려 왔다 (§4.3).
     *
     * 임대 만료가 뜻해야 하는 것은 "워커가 죽었다" 하나다. 브로커 큐에서 차례를
     * 기다리는 시간까지 임대에 들어가면, 밀린 것뿐인 멀쩡한 실행이 유실로 오해받아
     * 다시 돌고 결국 SYSTEM_ERROR 로 끝난다. 부하가 걸릴 때만 판정이 틀어지는 셈이라
     * 평소 테스트로는 드러나지 않는다.
     */
    fun onHeartbeat(heartbeat: ExecutionHeartbeat) {
        if (!registry.renew(heartbeat.submissionId, heartbeat.fencingToken)) {
            // 이미 무효가 된 워커다. 곧 결과를 보내겠지만 fencing 이 거절한다.
            log.atDebug()
                .addKeyValue(CorrelationIds.SUBMISSION_ID, heartbeat.submissionId)
                .addKeyValue(CorrelationIds.EXECUTION_ID, heartbeat.executionId)
                .log("스테일 워커의 심장 박동을 무시한다")
        }
    }

    /**
     * 만료된 임대를 회수한다 (§4.3 워커 유실, §12.4 고아 실행 회수).
     *
     * 워커가 죽으면 결과가 오지 않는다. 재임대는 attempt 와 fencing 토큰을 함께 올리므로,
     * 죽은 줄 알았던 워커가 살아 돌아와 결과를 보내도 토큰이 낮아 거절된다. 그래서
     * "다시 실행"과 "중복 판정"이 동시에 일어나지 않는다.
     */
    fun reclaimExpiredLeases() {
        for (expired in registry.expired()) {
            val submissionId = expired.submissionId

            if (expired.attempt >= maxAttempts) {
                registry.abandon(submissionId)
                log.atError()
                    .addKeyValue(CorrelationIds.SUBMISSION_ID, submissionId)
                    .addKeyValue(CorrelationIds.ATTEMPT, expired.attempt)
                    .log("재시도 한계를 넘었다. SYSTEM_ERROR 로 끝낸다")
                systemError(expired)
                continue
            }

            // 다른 인스턴스가 먼저 다시 걸었으면 물러난다. 같은 제출을 두 번 걸지 않는다.
            val lease = registry.reclaim(expired, executionId = UUID.randomUUID().toString()) ?: continue
            dispatch(lease)
        }
    }

    /** 임대가 품은 원 요청의 종류가 어느 판정기로 다시 걸지 정한다. */
    private fun dispatch(lease: Lease) {
        when (val origin = lease.origin) {
            is SubmissionQueued -> {
                val retry = request(lease, load(origin))
                metrics.leaseReclaimed(retry.language)
                log.atWarn()
                    .addKeyValue(CorrelationIds.SUBMISSION_ID, lease.submissionId)
                    .addKeyValue(CorrelationIds.EXECUTION_ID, retry.executionId)
                    .addKeyValue(CorrelationIds.ATTEMPT, retry.attempt)
                    .log("임대가 만료됐다. 실행을 다시 건다")
                gateway.requestExecution(retry)
            }
            is ProjectQueued -> projects?.dispatch(lease) ?: run {
                log.error("프로젝트 판정기가 없어 만료된 프로젝트 임대를 다시 걸지 못한다: {}", lease.submissionId)
                registry.abandon(lease.submissionId)
            }
        }
    }

    private fun systemError(expired: Lease) {
        when (expired.origin) {
            is SubmissionQueued -> gateway.publishCompleted(
                JudgeCompleted(
                    submissionId = expired.submissionId,
                    executionId = expired.executionId,
                    correlationId = expired.origin.correlationId,
                    verdict = Verdict.SYSTEM_ERROR,
                    score = 0,
                    compileLog = null,
                    groups = emptyList(),
                ),
            )
            is ProjectQueued -> projects?.systemError(expired)
        }
    }

    private fun load(origin: SubmissionQueued): ProblemPackage {
        val pkg = packages.load(origin.problemId)
        require(pkg.manifest.version == origin.problemVersion) {
            "요청한 문제 버전이 로드된 패키지와 다르다: ${origin.problemVersion} != ${pkg.manifest.version}"
        }
        return pkg
    }

    /**
     * 임대 하나가 곧 실행 요청 하나다. 다시 걸 때도 같은 길로 만든다.
     *
     * 테스트는 메시지에 싣지 않는다. 번들을 스토어에 올려 두고 참조만 실린다 (§8.3).
     */
    private fun request(lease: Lease, pkg: ProblemPackage): ExecutionRequest {
        val origin = lease.origin as SubmissionQueued
        return ExecutionRequest(
            executionId = lease.executionId,
            submissionId = lease.submissionId,
            attempt = lease.attempt,
            fencingToken = lease.token,
            correlationId = origin.correlationId,
            problemVersionId = pkg.problemVersionId,
            packageDigest = pkg.packageDigest,
            language = origin.language,
            // 소스는 참조로 지나간다 (§8.3). 이전 버전 메시지의 inline 소스도 그대로 넘긴다 (§15.3 N/N-1).
            source = origin.source,
            sourceRef = origin.sourceRef,
            signature = pkg.manifest.signature,
            limits = pkg.manifest.limits,
            bundle = bundles.ensure(pkg),
        )
    }

    /**
     * 판정이 끝난 뒤 학습용 트레이스를 별도 작업으로 띄운다 (§7.1).
     *
     * 저우선순위 큐로 분리하는 것이 다음 단계다. 지금은 같은 큐를 쓰되, 계측 오버헤드가
     * 판정 실행에 섞이지 않는다는 본질은 모드 분리로 지켜진다.
     *
     * **떨어진 케이스를 고른다.** 예전에는 언제나 첫 공개 케이스를 되짚었는데, 사용자가
     * 리플레이를 여는 이유는 대개 "어디서 틀렸나"이고 그 답은 통과한 케이스에 없다.
     * 떨어진 공개 케이스가 없으면 첫 공개 케이스로 돌아간다 — 정답을 받은 사람이
     * "내가 의도한 대로 돌았나"를 보려는 경우가 그쪽이다 (PRD §2.2).
     *
     * 숨은 케이스는 고르지 않는다. 그 상태 변화를 보여주면 테스트가 그대로 샌다 (§8.3).
     */
    private fun dispatchTrace(result: ExecutionResult, origin: SubmissionQueued, pkg: ProblemPackage) {
        val public = pkg.groups.filter { it.policy.exposesInput }
        if (public.isEmpty()) return

        val failedIds = result.cases
            .filter { it.verdict != Verdict.ACCEPTED }
            .map { it.groupId to it.caseId }
            .toSet()

        val chosen = public.firstNotNullOfOrNull { group ->
            group.cases.firstOrNull { (group.policy.id to it.id) in failedIds }
                ?.let { group.policy to it }
        } ?: public.first().let { group ->
            group.cases.firstOrNull()?.let { group.policy to it }
        } ?: return

        gateway.requestExecution(
            ExecutionRequest(
                executionId = UUID.randomUUID().toString(),
                submissionId = result.submissionId,
                attempt = result.attempt,
                fencingToken = result.fencingToken,
                correlationId = origin.correlationId,
                problemVersionId = pkg.problemVersionId,
                packageDigest = pkg.packageDigest,
                language = origin.language,
                source = origin.source,
                sourceRef = origin.sourceRef,
                signature = pkg.manifest.signature,
                limits = pkg.manifest.limits,
                mode = ExecutionMode.TRACE,
                bundle = bundles.ensure(pkg),
                selection = listOf(CaseSelection(chosen.first.id, chosen.second.id)),
            ),
        )
    }

    /** 결과 봉투가 말하는 문제 버전을 그대로 연다. `<id>@<version>` 형식이다. */
    private fun packageOf(result: ExecutionResult): ProblemPackage {
        val problemId = result.problemVersionId.substringBefore('@')
        require(problemId.isNotBlank()) {
            "결과 봉투에 문제 버전이 없다: ${result.executionId}"
        }
        val pkg = packages.load(problemId)
        require(pkg.problemVersionId == result.problemVersionId) {
            "채점한 문제 버전과 로드한 패키지가 다르다: ${result.problemVersionId} != ${pkg.problemVersionId}"
        }
        return pkg
    }
}

/** 조정자가 바깥으로 보내는 것들. 테스트에서는 가짜로 바꿔 끼운다. */
interface JudgeGateway {
    fun requestExecution(request: ExecutionRequest)
    fun publishProgress(progress: JudgeProgressed)
    fun publishCompleted(completed: JudgeCompleted)
    fun publishTraceReady(ready: TraceReady)
}
