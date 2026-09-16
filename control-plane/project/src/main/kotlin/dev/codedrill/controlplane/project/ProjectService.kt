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
            // 공개 테스트의 모듈 이름. 결과 화면이 "공개"와 "숨은"을 가르는 기준이다.
            publicTests = pkg.starter.keys.filter { it.startsWith("tests/test_") && it.endsWith(".py") }
                .map { it.removeSuffix(".py").replace('/', '.') },
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

    /** 종료. 같은 실행이 다시 와도 한 번만 적힌다. */
    @Transactional
    fun complete(message: ProjectCompleted): Boolean {
        val id = UUID.fromString(message.submissionId)
        val changed = repository.complete(
            id = id,
            executionId = message.executionId,
            verdict = message.verdict,
            score = message.score,
            log = message.log,
            tests = message.tests,
            hiddenPassed = message.hiddenPassed,
            hiddenTotal = message.hiddenTotal,
        )
        if (changed) {
            log.info("프로젝트 판정 완료: {} → {} ({}점, 숨은 {}/{})", id, message.verdict, message.score, message.hiddenPassed, message.hiddenTotal)
            signal(id, message)
        }
        return changed
    }

    /**
     * 판정을 증거로 (11단계). 실패해도 판정은 이미 적혔다 — 학습 기록은 판정보다 뒤의 관심사다.
     *
     * 더 쓴 테스트는 시작 저장소의 공개 테스트 파일과 제출의 같은 파일에서 `def test_` 를
     * 세어 뺀 것이다. 파일을 새로 만든 테스트도 센다 — `tests/test_*.py` 면 스위트가 돈다.
     */
    private fun signal(id: UUID, message: ProjectCompleted) {
        runCatching {
            val submission = repository.findById(id) ?: return
            val pkg = packages.load(submission.projectId)
            val starterTests = pkg.starter.filterKeys { it.isPublicTest() }.values.sumOf { it.testMethods() }
            // 숨은 테스트와 같은 경로에 쓴 것은 세지 않는다 — 채점 때 덮여 한 번도 돌지 않는다.
            val submittedTests = repository.files(id).filterKeys { it.isPublicTest() && it !in pkg.hidden }.values.sumOf { it.testMethods() }
            val added = (submittedTests - starterTests).coerceAtLeast(0)
            learning.judged(
                userId = submission.userId,
                projectId = submission.projectId,
                submissionId = id.toString(),
                accepted = message.verdict == dev.codedrill.judge.protocol.Verdict.ACCEPTED,
                addedTests = added,
                addedTestsPassed = message.tests.isNotEmpty() && message.tests.all { it.passed },
            )
        }.onFailure { log.warn("프로젝트 판정을 증거로 잇지 못했다: {} ({})", id, it.message) }
    }

    private fun String.isPublicTest() = startsWith("tests/test_") && endsWith(".py")
    private fun String.testMethods() = TEST_METHOD.findAll(this).count()

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

        /** unittest 가 찾는 이름. 들여쓰기 안의 메서드만 — 최상위 함수는 스위트가 돌리지 않는다. */
        private val TEST_METHOD = Regex("""^\s+def test_\w+\s*\(""", RegexOption.MULTILINE)

        /** 한 사람이 동시에 걸어 둘 수 있는 프로젝트 판정. 분 단위 실행이라 알고리즘 제출보다 좁다 (§10.2). */
        const val MAX_IN_FLIGHT = 2
    }
}
