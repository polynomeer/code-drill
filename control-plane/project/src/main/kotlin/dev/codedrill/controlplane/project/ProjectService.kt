package dev.codedrill.controlplane.project

import com.fasterxml.jackson.databind.ObjectMapper
import dev.codedrill.judge.protocol.Language
import dev.codedrill.judge.protocol.ProjectCompleted
import dev.codedrill.judge.protocol.ProjectQueued
import dev.codedrill.judge.protocol.Workspaces
import dev.codedrill.platform.common.IdempotencyKey
import dev.codedrill.platform.messaging.OutboxEvent
import dev.codedrill.platform.problempackage.Difficulty
import dev.codedrill.platform.problempackage.ProjectPackage
import dev.codedrill.platform.problempackage.ProjectPackageLoader
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

/**
 * 프로젝트형 문제의 조회와 제출 (feature-roadmap 11단계).
 *
 * 상세는 **시작 저장소만** 내보낸다. 숨은 테스트는 패키지에 있지만 어떤 응답에도 실리지
 * 않는다 — 여기가 그 경계다 (§8.3, §9.1).
 *
 * 제출은 알고리즘 제출과 같은 규칙이다: 파일을 스토어에 올리고 참조만 메시지에 싣되(B3),
 * 제출 행·파일·아웃박스 이벤트는 **한 트랜잭션**에 커밋한다 (§3.2).
 */
@Service
class ProjectService(
    private val packages: ProjectPackageLoader,
    private val published: PublishedProjects,
    private val repository: ProjectRepository,
    private val workspaces: WorkspaceStore,
    private val json: ObjectMapper,
    private val learning: ProjectLearningSignals = ProjectLearningSignals.NONE,
    private val rejudges: ProjectRejudgeContext = ProjectRejudgeContext.NONE,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    data class ProjectSummary(
        val id: String,
        val version: Int,
        val title: String,
        val language: String,
        val difficulty: Difficulty,
        val tags: List<String>,
        val summary: String,
        val solved: Boolean,
    )

    /** 상세. [files] 는 시작 저장소다 — 사용자가 편집기에서 여는 것. */
    data class ProjectView(
        val id: String,
        val version: Int,
        val title: String,
        val language: String,
        val difficulty: Difficulty,
        val tags: List<String>,
        val statement: String,
        val files: Map<String, String>,
        val limits: Limits,
        val publicTests: List<String>,
    ) {
        data class Limits(val buildSeconds: Int, val testSeconds: Int, val memoryMb: Int, val maxFiles: Int, val maxTotalBytes: Int)
    }

    fun list(userId: String?): List<ProjectSummary> {
        val solved = userId?.let(repository::solvedBy).orEmpty()
        return available().map { pkg ->
            ProjectSummary(
                id = pkg.manifest.id,
                version = pkg.manifest.version,
                title = pkg.manifest.title,
                language = pkg.manifest.language,
                difficulty = pkg.catalog.difficulty,
                tags = pkg.catalog.tags,
                summary = pkg.catalog.summary,
                solved = pkg.manifest.id in solved,
            )
        }
    }

    fun view(projectId: String): ProjectView? {
        val pkg = load(projectId) ?: return null
        return ProjectView(
            id = pkg.manifest.id,
            version = pkg.manifest.version,
            title = pkg.manifest.title,
            language = pkg.manifest.language,
            difficulty = pkg.catalog.difficulty,
            tags = pkg.catalog.tags,
            statement = pkg.statementMarkdown,
            files = pkg.starter,
            limits = ProjectView.Limits(
                pkg.manifest.limits.buildSeconds, pkg.manifest.limits.testSeconds, pkg.manifest.limits.memoryMb,
                Workspaces.MAX_FILES, Workspaces.MAX_TOTAL_BYTES,
            ),
            publicTests = pkg.publicModules,
        )
    }

    sealed interface SubmitOutcome {
        data class Accepted(val submission: ProjectSubmission) : SubmitOutcome
        data class Invalid(val reason: String) : SubmitOutcome
        data class Throttled(val reason: String) : SubmitOutcome
        data object NotFound : SubmitOutcome
    }

    /**
     * 제출. 같은 `(userId, idempotencyKey)` 로 다시 오면 기존 제출을 돌려준다 (§4.3).
     *
     * 파일은 [Workspaces.validate] 를 지나야 한다 — 경로가 밖을 가리키거나 너무 크면 스토어에
     * 올라가기 전에 거절된다. 숨은 테스트와 같은 경로의 파일은 받되 채점 때 덮인다; 거절하면
     * 어느 경로가 숨은 테스트인지 알려 주는 셈이다.
     */
    @Transactional
    fun submit(userId: String, idempotencyKey: String, projectId: String, files: Map<String, String>): SubmitOutcome {
        val pkg = load(projectId) ?: return SubmitOutcome.NotFound
        val key = IdempotencyKey(idempotencyKey)
        repository.findByIdempotencyKey(userId, key.value)?.let { return SubmitOutcome.Accepted(it) }

        val clean = try {
            Workspaces.validate(files)
        } catch (e: IllegalArgumentException) {
            return SubmitOutcome.Invalid(e.message ?: "파일이 올바르지 않다")
        }
        if (repository.inFlight(userId) >= MAX_IN_FLIGHT) {
            return SubmitOutcome.Throttled("채점 중인 프로젝트 제출이 ${MAX_IN_FLIGHT}건이다. 끝나기를 기다린다")
        }

        val id = UUID.randomUUID()
        val submission = ProjectSubmission(
            id = id,
            userId = userId,
            idempotencyKey = key.value,
            projectId = pkg.manifest.id,
            projectVersion = pkg.manifest.version,
            language = pkg.manifest.language,
            status = ProjectSubmission.Status.QUEUED,
        )
        // 스토어가 먼저다. 올리기가 실패하면 제출도 실패다 — 참조 없는 메시지를 내보내면 Runner 가
        // 시스템 오류로 끝내고, 그건 사용자에게 우리 탓으로 보여야 한다.
        val ref = workspaces.store(id.toString(), clean)
        val queued = ProjectQueued(
            submissionId = id.toString(),
            correlationId = UUID.randomUUID().toString(),
            projectId = pkg.manifest.id,
            projectVersion = pkg.manifest.version,
            language = Language.valueOf(pkg.manifest.language),
            workspace = ref,
            queuedAt = Instant.now(),
        )
        val inserted = repository.insertWithOutbox(
            submission, clean,
            OutboxEvent(
                id = UUID.randomUUID(),
                aggregate = "project-submission",
                aggregateId = id.toString(),
                type = PROJECT_EVENT,
                payload = json.writeValueAsString(queued),
                occurredAt = Instant.now(),
            ),
        )
        if (inserted == 0) {
            workspaces.delete(id.toString())
            return SubmitOutcome.Accepted(requireNotNull(repository.findByIdempotencyKey(userId, key.value)) { "멱등 충돌인데 기존 제출을 찾지 못했다" })
        }
        return SubmitOutcome.Accepted(submission)
    }

    // --- 초안 (§8.1) ---

    sealed interface DraftOutcome {
        data class Saved(val version: Long) : DraftOutcome
        data class Conflict(val current: ProjectDraft) : DraftOutcome
        data class Invalid(val reason: String) : DraftOutcome
    }

    fun draft(userId: String, projectId: String): ProjectDraft? = repository.findDraft(userId, projectId)

    /**
     * 초안 저장 (CAS). [expectedVersion] 이 null 이면 "아직 초안이 없다"는 주장이고, 그 주장이
     * 틀렸으면 새로 만들지 않고 충돌로 알린다 — Workspace 의 초안과 같은 규칙이다.
     *
     * 파일은 제출과 같은 검사를 지난다. 제출할 수 없는 것을 초안으로 받아 두면 제출하는
     * 순간에야 거절되고, 그때는 이미 시간을 쓴 뒤다.
     */
    @Transactional
    fun saveDraft(userId: String, projectId: String, files: Map<String, String>, expectedVersion: Long?): DraftOutcome {
        if (load(projectId) == null) return DraftOutcome.Invalid("없는 프로젝트다")
        val clean = try {
            Workspaces.validate(files)
        } catch (e: IllegalArgumentException) {
            return DraftOutcome.Invalid(e.message ?: "파일이 올바르지 않다")
        }
        if (expectedVersion == null) {
            if (repository.insertDraft(userId, projectId, clean) > 0) return DraftOutcome.Saved(1)
        } else if (repository.compareAndSetDraft(userId, projectId, clean, expectedVersion) > 0) {
            return DraftOutcome.Saved(expectedVersion + 1)
        }
        // 충돌 응답에는 현재 서버 상태를 함께 싣는다. 무엇과 충돌했는지 그 자리에서 보여야 고를 수 있다.
        val current = repository.findDraft(userId, projectId) ?: error("충돌인데 현재 초안을 찾지 못했다")
        return DraftOutcome.Conflict(current)
    }

    fun discardDraft(userId: String, projectId: String) = repository.deleteDraft(userId, projectId)

    fun find(id: UUID): ProjectSubmission? = repository.findById(id)

    fun files(id: UUID): Map<String, String> = repository.files(id)

    fun history(userId: String, projectId: String?, limit: Int?): List<ProjectSubmission> =
        repository.history(userId, projectId, (limit ?: 20).coerceIn(1, 100))

    /** 진행 알림. 되돌리지 않는다 — LEASED 가 QUEUED 뒤에 와도 종착 뒤에는 오지 않는다. */
    fun leased(id: UUID) {
        val current = repository.findById(id) ?: return
        if (current.status == ProjectSubmission.Status.QUEUED) {
            repository.transition(id, ProjectSubmission.Status.QUEUED, ProjectSubmission.Status.LEASED, current.version)
        }
    }

    /**
     * 채점 종료를 반영한다 (§4.2 INV-02). 알고리즘 제출과 같은 규칙이다.
     *
     * - 최초 판정: QUEUED/LEASED → COMPLETED 로 옮기고 판정을 채운다.
     * - 재채점: 이미 COMPLETED 인 행의 판정만 갈아 끼우고 revision 을 올린다.
     * - dry-run 재채점: 이력에만 남기고 현재 판정은 건드리지 않는다.
     * - 승인된 재채점 없이 끝난 제출에 온 결과(뒤늦은 실행)는 이력에만 남긴다 — 아무도 승인하지
     *   않은 판정 변경은 없다 (§11.2).
     *
     * 이력을 먼저 남긴다. 같은 실행이 다시 와도 이력의 유일 제약이 걸러 revision 이 헛되이 오르지
     * 않는다. 증거는 **최초 판정에서만** 쌓는다 — 재채점으로 바뀐 것은 사용자의 능력이 아니라
     * 문제 데이터가 바뀐 것이다.
     */
    @Transactional
    fun complete(message: ProjectCompleted): Boolean {
        val id = UUID.fromString(message.submissionId)
        val current = repository.findById(id) ?: return false
        val pending = rejudges.pendingFor(id)
        val first = current.status != ProjectSubmission.Status.COMPLETED
        val late = !first && pending == null
        val apply = pending?.dryRun != true && !late
        val revision = if (apply && !first) current.revision + 1 else current.revision

        val recorded = repository.recordJudgement(
            id, revision, message.executionId, message.verdict, message.score, message.log, message.tests,
            message.hiddenPassed, message.hiddenTotal, pending?.jobId, apply, message.probe,
        )
        if (recorded == 0) return false

        if (late) {
            log.warn("끝난 프로젝트 제출에 뒤늦은 결과가 왔다. 이력에만 남긴다: {} ({})", id, message.executionId)
            return true
        }
        if (apply) {
            repository.applyJudgement(
                id, revision, message.executionId, message.verdict, message.score, message.log, message.tests,
                message.hiddenPassed, message.hiddenTotal, message.probe,
            )
            log.info("프로젝트 판정 완료: {} → {} ({}점, 숨은 {}/{}, revision {})", id, message.verdict, message.score, message.hiddenPassed, message.hiddenTotal, revision)
        }
        if (pending != null) {
            rejudges.judged(
                ProjectRejudgeContext.Outcome(
                    submissionId = id,
                    jobId = pending.jobId,
                    applied = apply,
                    previousVerdict = current.verdict?.name,
                    previousScore = current.score,
                    verdict = message.verdict.name,
                    score = message.score,
                ),
            )
        }
        if (first) signal(id, message)
        return true
    }

    fun judgements(id: UUID): List<ProjectJudgement> = repository.judgements(id)

    // --- 재채점 (§8.1). Admin 이 무엇을 다시 돌릴지 정하고, 다시 거는 것은 여기다. ---

    fun completedFor(projectId: String): List<UUID> = repository.completedFor(projectId)

    fun completed(id: UUID): List<UUID> =
        listOfNotNull(repository.findById(id)?.takeIf { it.status == ProjectSubmission.Status.COMPLETED }?.id)

    /**
     * 다시 채점 큐에 올린다. DB 의 파일이 원본이라 스토어의 복제가 사라졌거나 다르면 다시 올린다
     * (§8.3). 같은 프로젝트 버전으로 돈다 — 그 버전이 더는 공개 버전이 아니어도, 판정의 근거는
     * 제출 당시의 것이다. 실제로 큐에 오른 건수를 돌려준다.
     */
    @Transactional
    fun requeue(ids: List<UUID>): Int = ids.count { id ->
        val submission = repository.findById(id) ?: return@count false
        val files = repository.files(id)
        if (files.isEmpty()) return@count false
        val ref = workspaces.ensure(id.toString(), files)
        repository.enqueueOutbox(
            OutboxEvent(
                id = UUID.randomUUID(),
                aggregate = "project-submission",
                aggregateId = id.toString(),
                type = PROJECT_EVENT,
                payload = json.writeValueAsString(
                    ProjectQueued(
                        submissionId = id.toString(),
                        correlationId = UUID.randomUUID().toString(),
                        projectId = submission.projectId,
                        projectVersion = submission.projectVersion,
                        language = Language.valueOf(submission.language),
                        workspace = ref,
                        queuedAt = Instant.now(),
                    ),
                ),
                occurredAt = Instant.now(),
            ),
        )
        true
    }

    /**
     * 판정을 증거와 대회 점수로 (11단계). 실패해도 판정은 이미 적혔다 — 학습 기록은 판정보다 뒤의 관심사다.
     *
     * 더 쓴 테스트는 시작 저장소의 공개 테스트 파일과 제출의 같은 파일에서 `def test_` 를
     * 세어 뺀 것이다. 파일을 새로 만든 테스트도 센다 — `tests/test_*.py` 면 스위트가 돈다.
     */
    private fun signal(id: UUID, message: ProjectCompleted) {
        runCatching {
            val submission = repository.findById(id) ?: return
            val pkg = packages.load(submission.projectId)
            val starterTests = pkg.starter.entries.filter { ProjectPackage.isTestModule(it.key) }.sumOf { ProjectPackage.testMethods(it.key, it.value) }
            // 숨은 테스트와 같은 경로에 쓴 것은 세지 않는다 — 채점 때 덮여 한 번도 돌지 않는다.
            val submittedTests = repository.files(id).entries
                .filter { ProjectPackage.isTestModule(it.key) && it.key !in pkg.hidden }
                .sumOf { ProjectPackage.testMethods(it.key, it.value) }
            val added = (submittedTests - starterTests).coerceAtLeast(0)
            learning.judged(
                userId = submission.userId,
                projectId = submission.projectId,
                submissionId = id.toString(),
                accepted = message.verdict == dev.codedrill.judge.protocol.Verdict.ACCEPTED,
                score = message.score,
                submittedAt = submission.createdAt,
                addedTests = added,
                addedTestsPassed = message.tests.isNotEmpty() && message.tests.all { it.passed },
                probe = message.probe?.let { ProjectLearningSignals.Probe(it.referencePassed, it.killed.size, it.killed.size + it.survived.size) },
            )
        }.onFailure { log.warn("프로젝트 판정을 증거로 잇지 못했다: {} ({})", id, it.message) }
    }


    private fun available(): List<ProjectPackage> {
        val ids = published.ids()
        return packages.ids().filter { it in ids }.mapNotNull { id -> runCatching { packages.load(id) }.getOrNull() }
    }

    private fun load(projectId: String): ProjectPackage? {
        if (projectId !in published.ids()) return null
        return runCatching { packages.load(projectId) }.getOrNull()
    }

    companion object {
        const val PROJECT_EVENT = "ProjectQueued"

        /** 한 사람이 동시에 걸어 둘 수 있는 프로젝트 판정. 분 단위 실행이라 알고리즘 제출보다 좁다 (§10.2). */
        const val MAX_IN_FLIGHT = 2
    }
}
