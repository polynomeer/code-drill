package dev.codedrill.judge.orchestrator

import dev.codedrill.judge.orchestrator.bundle.BundlePublisher
import dev.codedrill.judge.orchestrator.lease.Acceptance
import dev.codedrill.judge.orchestrator.lease.Lease
import dev.codedrill.judge.orchestrator.lease.LeaseRegistry
import dev.codedrill.judge.protocol.JudgeProgressed
import dev.codedrill.judge.protocol.JudgeStatus
import dev.codedrill.judge.protocol.ProjectCompleted
import dev.codedrill.judge.protocol.ProjectQueued
import dev.codedrill.judge.protocol.ProjectRequest
import dev.codedrill.judge.protocol.ProjectResult
import dev.codedrill.judge.protocol.Verdict
import dev.codedrill.platform.observability.CorrelationIds
import dev.codedrill.platform.problempackage.ProjectPackage
import dev.codedrill.platform.problempackage.ProjectPackageLoader
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

/**
 * 프로젝트형 판정의 조정자 (feature-roadmap 11단계 — 두 번째 판정기).
 *
 * [JudgeCoordinator] 와 **임대를 같이 쓴다.** 임대·fencing·심장 박동·회수는 저쪽의 것을 그대로
 * 타고, 여기에는 봉투를 만드는 법과 채점 규칙만 있다 — 테스트 리포트를 점수로 바꾸고, 숨은
 * 모듈의 내역을 잘라내는 것.
 *
 * 만료된 임대의 회수는 [JudgeCoordinator.reclaimExpiredLeases] 가 한 번에 훑고, 원 요청이
 * [ProjectQueued] 이면 [dispatch]·[systemError] 로 넘어온다.
 */
class ProjectCoordinator(
    private val packages: ProjectPackageLoader,
    private val registry: LeaseRegistry,
    private val gateway: ProjectGateway,
    private val suites: BundlePublisher,
    private val metrics: JudgeMetrics = JudgeMetrics(),
) {

    private val log = LoggerFactory.getLogger(javaClass)
    private val progressSeq = AtomicLong()

    fun onProjectQueued(message: ProjectQueued) {
        val pkg = load(message)
        val lease = registry.lease(message, executionId = UUID.randomUUID().toString())

        log.atInfo()
            .addKeyValue(CorrelationIds.SUBMISSION_ID, message.submissionId)
            .addKeyValue(CorrelationIds.EXECUTION_ID, lease.executionId)
            .addKeyValue(CorrelationIds.ATTEMPT, lease.attempt)
            .log("프로젝트 판정을 임대하고 Runner 에 요청한다")

        message.queuedAt?.let { metrics.queueWait(Duration.between(it, Instant.now())) }

        gateway.requestProject(request(lease, message, pkg))
        gateway.publishProjectProgress(
            JudgeProgressed(
                submissionId = message.submissionId,
                correlationId = message.correlationId,
                seq = progressSeq.incrementAndGet(),
                status = JudgeStatus.LEASED,
            ),
        )
    }

    fun onProjectResult(result: ProjectResult, correlationId: String) {
        val acceptance = registry.accept(result)
        metrics.acceptance(acceptance)

        when (acceptance) {
            is Acceptance.Accepted, Acceptance.Unleased -> complete(result, correlationId)
            Acceptance.Duplicate -> log.atInfo()
                .addKeyValue(CorrelationIds.SUBMISSION_ID, result.submissionId)
                .log("같은 프로젝트 결과가 다시 도착했다. no-op")
            Acceptance.AlreadyCompleted -> log.atWarn()
                .addKeyValue(CorrelationIds.SUBMISSION_ID, result.submissionId)
                .addKeyValue(CorrelationIds.EXECUTION_ID, result.executionId)
                .log("종료된 프로젝트 제출에 다른 결과가 도착했다. 감사 대상")
            is Acceptance.Stale -> log.atWarn()
                .addKeyValue(CorrelationIds.SUBMISSION_ID, result.submissionId)
                .addKeyValue(CorrelationIds.EXECUTION_ID, result.executionId)
                .log("스테일 프로젝트 결과를 폐기했다: ${acceptance.reason}")
        }
    }

    /**
     * 리포트를 판정으로.
     *
     * **숨은 모듈의 내역은 여기서 잘라낸다** (§8.3). 이름도 사유도 나가지 않고 통과 수만 남는다.
     * 알고리즘 판정이 숨은 그룹의 케이스 목록을 비우는 것과 같은 자리, 같은 이유다. 점수는
     * 통과한 테스트의 비율이다 — 공개·숨은 것을 가리지 않고 하나가 한 표다.
     */
    private fun complete(result: ProjectResult, correlationId: String) {
        val pkg = packageOf(result)
        val hidden = result.tests.filter { it.module in pkg.hiddenModules }
        val public = result.tests.filterNot { it.module in pkg.hiddenModules }
        val passed = result.tests.count { it.passed }

        gateway.publishProjectCompleted(
            ProjectCompleted(
                submissionId = result.submissionId,
                executionId = result.executionId,
                correlationId = correlationId,
                verdict = result.verdict,
                score = if (result.tests.isEmpty()) 0 else passed * 100 / result.tests.size,
                log = result.log,
                tests = public,
                hiddenPassed = hidden.count { it.passed },
                hiddenTotal = hidden.size,
            ),
        )
    }

    /** 만료된 임대를 새 실행으로 다시 건다. [JudgeCoordinator.reclaimExpiredLeases] 가 부른다. */
    fun dispatch(lease: Lease) {
        val origin = lease.origin as ProjectQueued
        val retry = request(lease, origin, load(origin))
        metrics.leaseReclaimed(origin.language)
        log.atWarn()
            .addKeyValue(CorrelationIds.SUBMISSION_ID, lease.submissionId)
            .addKeyValue(CorrelationIds.EXECUTION_ID, retry.executionId)
            .addKeyValue(CorrelationIds.ATTEMPT, retry.attempt)
            .log("프로젝트 임대가 만료됐다. 실행을 다시 건다")
        gateway.requestProject(retry)
    }

    /** 재시도 한계를 넘었다. 사용자에게도 경보에도 드러나야 한다 (§4.4). */
    fun systemError(expired: Lease) {
        gateway.publishProjectCompleted(
            ProjectCompleted(
                submissionId = expired.submissionId,
                executionId = expired.executionId,
                correlationId = expired.origin.correlationId,
                verdict = Verdict.SYSTEM_ERROR,
                score = 0,
                log = null,
                tests = emptyList(),
                hiddenPassed = 0,
                hiddenTotal = 0,
            ),
        )
    }

    private fun load(origin: ProjectQueued): ProjectPackage {
        val pkg = packages.load(origin.projectId)
        require(pkg.manifest.version == origin.projectVersion) {
            "요청한 프로젝트 버전이 로드된 패키지와 다르다: ${origin.projectVersion} != ${pkg.manifest.version}"
        }
        return pkg
    }

    private fun request(lease: Lease, origin: ProjectQueued, pkg: ProjectPackage) = ProjectRequest(
        executionId = lease.executionId,
        submissionId = lease.submissionId,
        attempt = lease.attempt,
        fencingToken = lease.token,
        correlationId = origin.correlationId,
        projectVersionId = pkg.projectVersionId,
        packageDigest = pkg.packageDigest,
        language = origin.language,
        workspace = origin.workspace,
        suite = suites.ensureSuite(pkg),
        limits = pkg.manifest.limits,
    )

    private fun packageOf(result: ProjectResult): ProjectPackage {
        val projectId = result.projectVersionId.substringBefore('@')
        require(projectId.isNotBlank()) { "결과 봉투에 프로젝트 버전이 없다: ${result.executionId}" }
        val pkg = packages.load(projectId)
        require(pkg.projectVersionId == result.projectVersionId) {
            "채점한 프로젝트 버전과 로드한 패키지가 다르다: ${result.projectVersionId} != ${pkg.projectVersionId}"
        }
        return pkg
    }
}

/** 프로젝트형 조정자가 바깥으로 보내는 것들. 테스트에서는 가짜로 바꿔 끼운다. */
interface ProjectGateway {
    fun requestProject(request: ProjectRequest)
    fun publishProjectProgress(progress: JudgeProgressed)
    fun publishProjectCompleted(completed: ProjectCompleted)
}
