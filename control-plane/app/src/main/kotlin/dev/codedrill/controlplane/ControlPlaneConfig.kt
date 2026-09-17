package dev.codedrill.controlplane

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import dev.codedrill.judge.protocol.ExecutionRequest
import dev.codedrill.judge.protocol.ArenaRequest
import dev.codedrill.judge.protocol.LabRequest
import dev.codedrill.judge.protocol.MutationRequest
import dev.codedrill.judge.protocol.ShrinkRequest
import dev.codedrill.judge.protocol.SubmissionQueued
import dev.codedrill.platform.messaging.JudgeQueues
import dev.codedrill.controlplane.outbox.OutboxRoute
import dev.codedrill.controlplane.outbox.OutboxRoutes
import dev.codedrill.controlplane.admin.JudgedSubmissions
import dev.codedrill.controlplane.admin.PublishService
import dev.codedrill.controlplane.admin.RejudgeResult
import dev.codedrill.controlplane.admin.RejudgeService
import dev.codedrill.controlplane.admin.AdminAccounts
import dev.codedrill.controlplane.submission.QuotaLimits
import org.springframework.boot.context.properties.EnableConfigurationProperties
import dev.codedrill.controlplane.identity.IdentityService
import dev.codedrill.controlplane.competency.CompetencyService
import dev.codedrill.controlplane.competency.MasteryLevel
import dev.codedrill.controlplane.competency.EvidenceRepository
import dev.codedrill.controlplane.coaching.CoachableProblems
import dev.codedrill.controlplane.coaching.CoachingRepository
import dev.codedrill.controlplane.coaching.CoachingService
import dev.codedrill.controlplane.coaching.Diagnosis
import dev.codedrill.controlplane.coaching.HintLadder
import dev.codedrill.controlplane.coaching.SolvedProblems
import dev.codedrill.controlplane.coaching.TransferRepository
import dev.codedrill.controlplane.coaching.TransferService
import dev.codedrill.controlplane.coaching.TransferSignals
import dev.codedrill.controlplane.identity.PersonalData
import dev.codedrill.controlplane.learning.Attempt
import dev.codedrill.controlplane.learning.LearningRepository
import dev.codedrill.controlplane.learning.LearningSources
import dev.codedrill.controlplane.learning.Standing
import dev.codedrill.controlplane.submission.LearningSignals as SubmissionLearningSignals
import dev.codedrill.controlplane.workspace.LearningSignals as WorkspaceLearningSignals
import dev.codedrill.platform.problempackage.Competency
import dev.codedrill.platform.problempackage.DefectKind
import dev.codedrill.controlplane.submission.SubmissionPersonalData
import dev.codedrill.controlplane.submission.SubmissionRepository
import dev.codedrill.controlplane.trace.TraceRetentionPolicy
import dev.codedrill.controlplane.workspace.DraftPersonalData
import dev.codedrill.controlplane.workspace.PreQuestionRepository
import dev.codedrill.controlplane.workspace.PreQuestions
import dev.codedrill.controlplane.admin.ArenaModeration
import dev.codedrill.controlplane.admin.DiscussionModeration
import dev.codedrill.controlplane.admin.IntegrityModeration
import dev.codedrill.controlplane.admin.SanctionModeration
import dev.codedrill.controlplane.admin.ContestAdministration
import dev.codedrill.controlplane.contest.ContestProblems
import dev.codedrill.controlplane.contest.ContestRepository
import dev.codedrill.controlplane.contest.ContestService
import dev.codedrill.controlplane.identity.SanctionKind
import dev.codedrill.controlplane.identity.SanctionRepository
import dev.codedrill.controlplane.identity.SanctionService
import dev.codedrill.controlplane.integrity.IntegrityRepository
import dev.codedrill.controlplane.integrity.IntegrityService
import dev.codedrill.controlplane.integrity.SubmissionSources
import dev.codedrill.controlplane.submission.lab.SharedApproach
import dev.codedrill.controlplane.submission.lab.SharedApproaches
import dev.codedrill.controlplane.submission.SharedSubmissions
import dev.codedrill.controlplane.submission.SourceStore
import dev.codedrill.judge.protocol.SourceRef
import dev.codedrill.judge.protocol.Sources
import dev.codedrill.platform.storage.BlobStore
import dev.codedrill.controlplane.project.ProjectLearningSignals
import dev.codedrill.controlplane.project.ProjectPersonalData
import dev.codedrill.controlplane.project.ProjectRejudgeContext
import dev.codedrill.controlplane.project.ProjectService
import dev.codedrill.controlplane.project.PublishedProjects
import dev.codedrill.controlplane.project.WorkspaceStore
import dev.codedrill.judge.protocol.ProjectQueued
import dev.codedrill.judge.protocol.WorkspaceRef
import dev.codedrill.judge.protocol.Workspaces
import dev.codedrill.platform.problempackage.ProjectPackageLoader
import dev.codedrill.controlplane.workspace.AnchorableSubmissions
import dev.codedrill.controlplane.workspace.ApprovedDonations
import dev.codedrill.controlplane.workspace.DiscussionRepository
import dev.codedrill.controlplane.workspace.DiscussionService
import dev.codedrill.controlplane.workspace.ArenaGate
import dev.codedrill.controlplane.workspace.CommunityMutantRepository
import dev.codedrill.controlplane.workspace.CommunityMutantService
import dev.codedrill.controlplane.workspace.DonatableSubmission
import dev.codedrill.controlplane.workspace.DonatableSubmissions
import dev.codedrill.controlplane.submission.Submission
import dev.codedrill.judge.protocol.Verdict
import dev.codedrill.controlplane.workspace.ArenaRepository
import dev.codedrill.controlplane.workspace.ArenaService
import dev.codedrill.controlplane.workspace.MutationLimits
import dev.codedrill.controlplane.workspace.MutationRepository
import dev.codedrill.controlplane.submission.lab.LabRepository
import dev.codedrill.controlplane.submission.lab.LabService
import dev.codedrill.controlplane.submission.trace.CounterexampleService
import dev.codedrill.controlplane.submission.trace.DivergenceService
import dev.codedrill.controlplane.submission.trace.PredictionRepository
import dev.codedrill.controlplane.workspace.MutationService
import dev.codedrill.controlplane.workspace.TrialLimits
import dev.codedrill.controlplane.workspace.TrialRepository
import dev.codedrill.controlplane.workspace.TrialService
import dev.codedrill.controlplane.problem.ProblemProgress
import dev.codedrill.controlplane.problem.PublishedProblems
import dev.codedrill.controlplane.submission.RejudgeContext
import dev.codedrill.controlplane.submission.SubmissionService
import dev.codedrill.platform.problempackage.ProblemPackageLoader
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.scheduling.annotation.EnableScheduling
import java.nio.file.Path
import java.util.UUID

@Configuration
@EnableConfigurationProperties(
    QuotaLimits::class,
    TraceRetentionPolicy::class,
    TrialLimits::class,
    MutationLimits::class,
)
@EnableScheduling
class ControlPlaneConfig {

    // ObjectMapper 는 Spring Boot 가 만든 것을 그대로 쓴다. 여기서 새로 만들면 Boot 가
    // 등록해 주는 JavaTimeModule 이 빠져 Instant 직렬화가 런타임에 깨진다.

    @Bean
    fun problemPackageLoader(@Value("\${codedrill.content.root}") root: String) =
        ProblemPackageLoader(Path.of(root))

    /**
     * Problem 모듈이 공개 여부를 묻는 창구를 Admin 의 상태에 연결한다 (§3.1 조립 지점).
     *
     * 두 모듈은 서로를 모른 채로 남고, 둘을 잇는 결정은 여기 한 줄에 모인다.
     */
    @Bean
    fun publishedProblems(publish: PublishService) = PublishedProblems { publish.publishedProblemIds() }

    /**
     * 문제 목록의 정답률과 완료 상태를 제출 도메인에 묻는다 (§3.1 조립 지점).
     *
     * Problem 은 제출 스키마를 모르고, Submission 은 목록이 있다는 것을 모른다. 둘을 아는
     * 곳은 여기 하나뿐이다.
     *
     * **매 요청마다 집계한다.** 제출이 수만 건인 지금은 그것으로 충분하고, 캐시를 먼저
     * 넣으면 목록이 언제 낡은 값을 보이는지가 또 하나의 문제가 된다. 이 집계가 아파지면
     * 그때 투영 테이블로 옮긴다 — 그 시점은 `problem_accuracy` 를 만드는 마이그레이션
     * 하나이며, 이 인터페이스는 그대로다.
     */
    @Bean
    fun problemProgress(jdbc: JdbcTemplate) = object : ProblemProgress {

        override fun accuracy(): Map<String, ProblemProgress.Accuracy> = jdbc.query(
            """
            SELECT problem_id,
                   count(DISTINCT user_id)                                   AS attempted,
                   count(DISTINCT user_id) FILTER (WHERE verdict = 'ACCEPTED') AS solved
              FROM submission
             WHERE status = 'COMPLETED'
             GROUP BY problem_id
            """.trimIndent(),
        ) { rs, _ ->
            rs.getString("problem_id") to ProblemProgress.Accuracy(
                attempted = rs.getInt("attempted"),
                solved = rs.getInt("solved"),
            )
        }.toMap()

        override fun solvedBy(userId: String): Set<String> = jdbc.query(
            "SELECT DISTINCT problem_id FROM submission WHERE user_id = ? AND verdict = 'ACCEPTED'",
            { rs, _ -> rs.getString("problem_id") },
            userId,
        ).toSet()
    }

    /**
     * 관리자 역할 부트스트랩이 쓰는 계정 조회 (§11.2).
     *
     * Admin 모듈은 Identity 를 참조하지 않는다 (§3.1). 첫 역할을 누구에게 줄지 정하는
     * 데 이메일 → 계정 id 하나가 필요할 뿐이라, 그 한 줄만 포트로 열고 잇는다.
     */
    @Bean
    fun adminAccounts(identity: IdentityService) = AdminAccounts { identity.findIdByEmail(it) }

    /**
     * 개인 데이터를 가진 모듈을 Identity 에 잇는다 (§11.3, §3.1).
     *
     * Identity 는 제출도 초안도 모른다. 그런데 "내 데이터를 지워 달라"에 답해야 하는
     * 것은 계정만이 아니므로, 각 모듈이 자기 몫을 내놓고 여기서 모은다.
     *
     * **모듈이 개인 데이터를 갖게 되면 이 목록에 넣어야 한다.** 넣지 않으면 반출은
     * 조용히 빠뜨리고 삭제는 조용히 남긴다.
     */
    /**
     * 제출 소스가 실행 영역으로 가는 길 (§8.3). 스토어에 올리고 메시지에는 참조만.
     *
     * digest 를 함께 저장해 두어, 재채점이 다시 올릴지를 내용을 받지 않고 묻는다 — 번들과 같다.
     */
    @Bean
    fun sourceStore(store: BlobStore) = object : SourceStore {
        override fun store(submissionId: String, source: String): SourceRef {
            val bytes = Sources.bytes(source)
            val ref = SourceRef(Sources.key(submissionId), Sources.digest(bytes))
            store.put(ref.key, bytes, Sources.CONTENT_TYPE, ref.digest)
            return ref
        }

        override fun ensure(submissionId: String, source: String): SourceRef {
            val bytes = Sources.bytes(source)
            val ref = SourceRef(Sources.key(submissionId), Sources.digest(bytes))
            if (store.digestOf(ref.key) != ref.digest) store.put(ref.key, bytes, Sources.CONTENT_TYPE, ref.digest)
            return ref
        }

        override fun delete(submissionId: String) = store.delete(Sources.key(submissionId))
    }

    // --- 프로젝트형 문제 (feature-roadmap 11단계) ---

    /** 알고리즘 문제의 형제 디렉터리. 오케스트레이터도 같은 곳을 읽는다. */
    @Bean
    fun projectPackageLoader(@Value("\${codedrill.content.projects-root}") root: String) =
        ProjectPackageLoader(Path.of(root))

    /** 프로젝트형 문제도 문제다 — 같은 표에 등록·공개된다 (§3.2). 디렉터리가 다르니 겹치지 않는다. */
    @Bean
    fun publishedProjects(publish: PublishService) = PublishedProjects { publish.publishedProblemIds() }

    /**
     * 워크스페이스가 실행 영역으로 가는 길 (§8.3). 소스 스토어와 같은 분담이다 — DB 의 파일이
     * 원본, 스토어의 것은 실행용 복제.
     */
    @Bean
    fun workspaceStore(store: BlobStore) = object : WorkspaceStore {
        override fun store(submissionId: String, files: Map<String, String>): WorkspaceRef {
            val bytes = Workspaces.encode(files)
            val ref = WorkspaceRef(Workspaces.workspaceKey(submissionId), Workspaces.digest(bytes))
            store.put(ref.key, bytes, Workspaces.CONTENT_TYPE, ref.digest)
            return ref
        }

        override fun ensure(submissionId: String, files: Map<String, String>): WorkspaceRef {
            val bytes = Workspaces.encode(files)
            val ref = WorkspaceRef(Workspaces.workspaceKey(submissionId), Workspaces.digest(bytes))
            if (store.digestOf(ref.key) != ref.digest) store.put(ref.key, bytes, Workspaces.CONTENT_TYPE, ref.digest)
            return ref
        }

        override fun delete(submissionId: String) = store.delete(Workspaces.workspaceKey(submissionId))
    }

    /** 프로젝트형 재채점 (11단계). 알고리즘 제출과 같은 두 포트를 프로젝트 쪽에 하나씩 더 잇는다. */
    @Bean
    fun judgedProjects(projects: ObjectProvider<ProjectService>) = object : JudgedSubmissions {
        override fun completedFor(problemId: String) = projects.getObject().completedFor(problemId)
        override fun completed(submissionId: UUID) = projects.getObject().completed(submissionId)
        override fun requeue(ids: List<UUID>) = projects.getObject().requeue(ids)
    }

    @Bean
    fun projectRejudgeContext(rejudge: RejudgeService) = object : ProjectRejudgeContext {
        override fun pendingFor(submissionId: UUID) =
            rejudge.pendingFor(submissionId)?.let { ProjectRejudgeContext.Pending(jobId = it.jobId, dryRun = it.dryRun) }

        override fun judged(outcome: ProjectRejudgeContext.Outcome) = rejudge.judged(
            RejudgeResult(
                submissionId = outcome.submissionId,
                jobId = outcome.jobId,
                applied = outcome.applied,
                previousVerdict = outcome.previousVerdict,
                previousScore = outcome.previousScore,
                verdict = outcome.verdict,
                score = outcome.score,
            ),
        )
    }

    /**
     * 프로젝트형 판정 → 실무군 증거와 대회 점수 (11단계). 세 모듈은 서로를 모른다.
     *
     * 프로젝트형 문제도 문제라 대회에 들어간다 — 공개된 id 면 대회 문제로 받는다. 판정이 오면
     * 알고리즘 제출과 같은 자리에서 같은 창(제출 시각)으로 점수를 적는다 (§8.4).
     */
    @Bean
    fun projectLearningSignals(service: CompetencyService, contests: ContestService) =
        ProjectLearningSignals { userId, projectId, submissionId, accepted, score, submittedAt, addedTests, addedTestsPassed ->
            runCatching { contests.judged(userId, projectId, score, accepted, submittedAt) }
            service.projectJudged(userId, projectId, submissionId, accepted, addedTests, addedTestsPassed)
        }

    @Bean
    fun projectPersonalArea(data: ProjectPersonalData) = object : PersonalData {
        override val area = "projects"
        override fun export(userId: String) = data.export(userId)
        override fun erase(userId: String) = data.erase(userId)
    }

    @Bean
    fun submissionPersonalArea(data: SubmissionPersonalData) = object : PersonalData {
        override val area = "submissions"
        override fun export(userId: String) = data.export(userId)
        override fun erase(userId: String) = data.erase(userId)
    }

    @Bean
    fun draftPersonalArea(data: DraftPersonalData) = object : PersonalData {
        override val area = "drafts"
        override fun export(userId: String) = data.export(userId)
        override fun erase(userId: String) = data.erase(userId)
    }

    /**
     * 시험 실행도 사용자가 적은 것이다 — 소스와 입력이 그대로 들어 있다.
     *
     * 새 표를 만들면서 이 목록에 넣는 것을 잊으면, 반출은 조용히 빠뜨리고 삭제는 조용히
     * 남긴다. 둘 다 오류를 내지 않는다.
     */
    /**
     * 풀이 전 질문이 고를 수 있는 기법 (FR-803).
     *
     * `content/tags.yaml` 에서 **containers 를 뺀 나머지**다. 목록을 코드에 또 적으면
     * 태그를 하나 늘렸을 때 선택지에는 없는 정답이 생기고, 사용자는 무엇을 골라도 틀리는
     * 질문을 받는다.
     *
     * containers(배열·문자열·격자)를 빼는 이유는 그것이 답이 되지 못하기 때문이다 —
     * 배열 문제를 "배열로 푼다"고 답하는 것은 아무 말도 아니다.
     */
    @Bean
    fun preQuestions(@Value("\${codedrill.content.root}") root: String): PreQuestions {
        val file = Path.of(root).parent?.resolve("tags.yaml")
        val text = file?.takeIf { it.toFile().isFile }?.toFile()?.readText().orEmpty()

        fun section(name: String): List<String> =
            Regex("$name:\\s*\\n((?:\\s+- \\S+\\n?)+)").find(text)
                ?.groupValues?.get(1)
                ?.let { block -> Regex("- (\\S+)").findAll(block).map { it.groupValues[1] }.toList() }
                .orEmpty()

        return PreQuestions(section("structures") + section("techniques"))
    }

    /** 풀이 전 응답의 근거도 사용자가 적은 것이다 (§11.3, FR-803). */
    @Bean
    fun preQuestionPersonalArea(repository: PreQuestionRepository) = object : PersonalData {
        override val area = "prequestions"
        override fun export(userId: String) = mapOf("answers" to repository.export(userId))
        override fun erase(userId: String) = mapOf("rationales" to repository.erase(userId))
    }

    @Bean
    fun trialPersonalArea(repository: TrialRepository) = object : PersonalData {
        override val area = "trials"
        override fun export(userId: String) = mapOf("runs" to repository.export(userId))
        override fun erase(userId: String) = mapOf("runs" to repository.erase(userId))
    }

    /** 해설 열람과 실험 입력도 사용자의 것이다 (§11.3). */
    @Bean
    fun labPersonalArea(repository: LabRepository) = object : PersonalData {
        override val area = "labs"
        override fun export(userId: String) = repository.export(userId)
        override fun erase(userId: String) = repository.erase(userId)
    }

    /** 예측의 근거도 사용자가 쓴 것이다 (§11.3). */
    @Bean
    fun predictionPersonalArea(repository: PredictionRepository) = object : PersonalData {
        override val area = "predictions"
        override fun export(userId: String) = mapOf("predictions" to repository.export(userId))
        override fun erase(userId: String) = mapOf("rationales" to repository.erase(userId))
    }

    @Bean
    fun mutationPersonalArea(repository: MutationRepository) = object : PersonalData {
        override val area = "mutations"
        override fun export(userId: String) = mapOf("evaluations" to repository.export(userId))
        override fun erase(userId: String) = mapOf("evaluations" to repository.erase(userId))
    }

    /**
     * 판정과 작업 공간의 사건을 역량 증거로 잇는다 (§3.1 조립 지점, FR-801).
     *
     * 세 모듈이 서로를 모른 채로 남는다. 제출은 역량을 모르고, Competency 는 제출 스키마를
     * 모르며, 둘을 아는 곳은 여기 하나뿐이다.
     *
     * **어느 쪽도 이 연결 때문에 멈추지 않는다.** 부르는 쪽이 예외를 삼키고, 기본 구현은
     * 아무 일도 하지 않는 NONE 이다 — 학습 기록은 판정보다 뒤에 있는 관심사다.
     */
    @Bean
    fun submissionLearningSignals(
        service: CompetencyService,
        coaching: CoachingService,
        transfers: TransferService,
        lab: LabService,
        integrity: IntegrityService,
        submissions: SubmissionRepository,
        contests: ContestService,
    ) =
        object : SubmissionLearningSignals {
            override fun judged(
                userId: String,
                problemId: String,
                submissionId: java.util.UUID,
                accepted: Boolean,
            ) {
                // 대회 중의 판정은 그 대회의 점수다 (§8.4). 제출 시각으로 창을 본다 — 판정이 늦어도 제출은 대회 안이다.
                runCatching {
                    submissions.findById(submissionId)?.let { contests.judged(userId, problemId, it.score ?: 0, accepted, it.createdAt) }
                }
                // 맞힌 제출은 유사도 신호의 재료다 (§11.4). 학습 기록과 같은 자리에서 같은 이유로
                // 실패를 삼킨다 — 신호가 판정을 막아서는 안 된다.
                if (accepted) runCatching {
                    val submission = submissions.findById(submissionId)
                    val source = submissions.findSource(submissionId)
                    if (submission != null && source != null) integrity.accepted(userId, problemId, submissionId, submission.language, source)
                }
                // 이 문제에서 받은 도움을 여기서 조회해 넘긴다 (FR-806). 제출 모듈은
                // 코칭을 모르고 Competency 는 세션을 모르며, 둘을 아는 곳은 여기뿐이다.
                // 정답 전에 해설을 열었으면 방법을 본 것이다 — 힌트 3단계와 같다 (FR-214).
                val helpLevel = maxOf(
                    runCatching { coaching.helpLevel(userId, problemId) }.getOrDefault(0),
                    if (runCatching { lab.unlockedEarly(userId, problemId) }.getOrDefault(false)) EDITORIAL_HELP else 0,
                )
                service.judged(userId, problemId, submissionId.toString(), accepted, helpLevel = helpLevel)
                // 이 판정이 누군가의 전이 확인 과제를 닫을 수 있다 (FR-807). 실패해도
                // 판정을 막지 않는다 — 학습 기록은 판정보다 뒤에 있는 관심사다.
                runCatching { transfers.judged(userId, problemId, accepted) }
            }

            override fun predicted(
                userId: String,
                problemId: String,
                predictionId: String,
                correct: Boolean,
            ) = service.predicted(userId, problemId, predictionId, correct)
        }

    /**
     * 처방이 기대는 사실들 (§3.1 조립 지점, FR-808).
     *
     * 기획서 §8.1 이 추천에 넣으라고 한 넷 — 최근 오답 원인·힌트 의존도·복습 간격·전이
     * 성과 — 가 각각 제출·코칭·제출·코칭 도메인의 사실이고, Learning 은 그 어느 것도 직접
     * 읽지 않는다. 넷을 아는 곳은 여기 하나뿐이다.
     */
    @Bean
    fun learningSources(
        jdbc: JdbcTemplate,
        progress: ProblemProgress,
        coaching: CoachingService,
        transfers: TransferRepository,
        competency: CompetencyService,
        publish: PublishService,
    ) = object : LearningSources {
        override fun attempts(userId: String, since: java.time.Instant): List<Attempt> = jdbc.query(
            """
            SELECT problem_id, verdict, created_at FROM submission
             WHERE user_id = ? AND status = 'COMPLETED' AND verdict IS NOT NULL AND created_at >= ?
             ORDER BY created_at
            """.trimIndent(),
            { rs, _ ->
                Attempt(
                    problemId = rs.getString("problem_id"),
                    accepted = rs.getString("verdict") == "ACCEPTED",
                    verdict = rs.getString("verdict"),
                    at = rs.getTimestamp("created_at").toInstant(),
                )
            },
            userId, java.sql.Timestamp.from(since),
        )

        override fun solved(userId: String) = progress.solvedBy(userId)
        override fun helpLevel(userId: String, problemId: String) = coaching.helpLevel(userId, problemId)
        override fun pendingTransfer(userId: String) = transfers.pendingTarget(userId)

        // 등급 이름으로 옮긴다. Learning 이 Competency 의 타입을 알면 안 된다 (§3.1).
        override fun standing(userId: String, asOf: java.time.Instant): Map<Competency, Standing> =
            competency.mapOf(userId, asOf).associate { it.competency to Standing.valueOf(it.level.name) }

        override fun published() = publish.publishedProblemIds()
    }

    /** 문제집과 처방 조정도 사용자의 것이다 (§11.3). */
    @Bean
    fun learningPersonalArea(repository: LearningRepository) = object : PersonalData {
        override val area = "learning"
        override fun export(userId: String) = repository.export(userId)
        override fun erase(userId: String) = repository.erase(userId)
    }

    /**
     * 코칭이 "무엇이 약한가"를 묻는 창구 (§3.1 조립 지점, FR-802).
     *
     * 약한 순서는 **등급 먼저, 증거 수 나중**이다. 아직 재지 않은 역량은 못하는 것이
     * 아니라 모르는 것이므로 DEVELOPING 뒤에 서고, STRONG 은 아예 빠진다 — 잘하는 것에
     * 힌트를 붙이면 그것은 개입이 아니라 방해다.
     */
    @Bean
    fun coachingDiagnosis(service: CompetencyService) = Diagnosis { userId, among, limit ->
        if (among.isEmpty()) {
            emptyList()
        } else {
            val map = service.mapOf(userId).associateBy { it.competency }
            among.mapNotNull { map[it] }
                .filter { it.level != MasteryLevel.STRONG }
                .sortedWith(compareBy({ NEEDS_HELP.indexOf(it.level) }, { it.evidenceCount }))
                .take(limit)
                .map { it.competency }
        }
    }

    @Bean
    fun coachingHintLadder(packages: ProblemPackageLoader) = HintLadder(packages)

    /**
     * 변형 문제로 낼 수 있는 문제 (§3.1 조립 지점, FR-807).
     *
     * 공개된 것만 낸다. 콘텐츠 디렉터리를 훑으면 아직 공개되지 않은 문제가 과제로 나가고,
     * 사용자는 열 수 없는 문제를 풀라는 말을 듣는다.
     */
    @Bean
    fun coachableProblems(publish: PublishService) = CoachableProblems { publish.publishedProblemIds() }

    /** 이미 푼 문제는 변형 과제가 되지 않는다. 옮겨졌는지는 안 풀어 본 문제에서만 드러난다. */
    @Bean
    fun coachingSolvedProblems(progress: ProblemProgress) = SolvedProblems { progress.solvedBy(it) }

    /** 확인된 전이를 가장 무거운 증거로 남긴다 (FR-807). */
    @Bean
    fun coachingTransferSignals(service: CompetencyService) =
        TransferSignals { userId, problemId, taskId, competencies ->
            service.transferred(userId, problemId, taskId, competencies)
        }

    /** 전이 확인도 사용자의 기록이다 (§11.3). 세션을 지우면 과제도 함께 사라진다. */
    @Bean
    fun transferPersonalArea(repository: TransferRepository) = object : PersonalData {
        override val area = "transfers"
        override fun export(userId: String) = mapOf("tasks" to repository.export(userId))

        // 지우기는 coaching 이 맡는다 — transfer_task 는 세션을 ON DELETE CASCADE 로
        // 따라간다. 여기서 또 지우면 이미 없는 것을 지우고 0 을 보고한다.
        override fun erase(userId: String) = mapOf("tasks" to 0)
    }

    /** 무엇을 도움받았는지도 사용자의 기록이다 (§11.3). */
    @Bean
    fun coachingPersonalArea(repository: CoachingRepository) = object : PersonalData {
        override val area = "coaching"
        override fun export(userId: String) = mapOf("sessions" to repository.export(userId))
        override fun erase(userId: String) = mapOf("sessions" to repository.erase(userId))
    }

    @Bean
    fun workspaceLearningSignals(service: CompetencyService, contests: ContestService) = object : WorkspaceLearningSignals {
        override fun answered(
            userId: String,
            problemId: String,
            competency: Competency,
            correct: Boolean,
            reference: String,
            detail: String?,
        ) = service.answered(userId, problemId, competency, correct, reference, detail)

        override fun tested(userId: String, problemId: String, trialId: String, judgedCases: Int) =
            service.tested(userId, problemId, trialId, judgedCases)

        override fun mutationChecked(
            userId: String,
            problemId: String,
            evaluationId: String,
            killedByKind: Map<DefectKind, Pair<Int, Int>>,
        ) = service.mutationChecked(userId, problemId, evaluationId, killedByKind)

        override fun brokeMutants(
            userId: String,
            problemId: String,
            attemptId: String,
            broken: Int,
            total: Int,
            kinds: List<DefectKind>,
            targets: List<String>,
            at: java.time.Instant,
        ) {
            service.brokeMutants(userId, problemId, attemptId, broken, total)
            // 반례 대전 중에 깨뜨린 과녁은 그 대회의 점수다 (§8.4). 학습 기록을 막지 않는다.
            runCatching { contests.hacked(userId, problemId, targets, at) }
        }
    }

    /** 아레나의 잠금 — 이 문제를 맞혔나 (§3.1 조립 지점, §8.3). */
    @Bean
    fun arenaGate(submissions: SubmissionRepository) =
        ArenaGate { userId, problemId -> submissions.latestAccepted(userId, problemId) != null }

    /** 아레나 시도도 사용자의 것이다 (§11.3). */
    @Bean
    fun arenaPersonalArea(repository: ArenaRepository) = object : PersonalData {
        override val area = "arena"
        override fun export(userId: String) = mapOf("attempts" to repository.export(userId))
        override fun erase(userId: String) = mapOf("attempts" to repository.erase(userId))
    }

    /**
     * 아레나에 내놓을 수 있는 제출 (§3.1 조립 지점, §8.3 익명화된 오답).
     *
     * 아레나가 돌리는 오답은 Kotlin 이다 — 저작자의 대표 오답과 같은 길로 컴파일한다. 다른
     * 언어의 오답은 그 길이 생길 때 열린다.
     */
    @Bean
    fun donatableSubmissions(submissions: SubmissionRepository) = object : DonatableSubmissions {
        override fun donatable(userId: String, problemId: String) =
            submissions.wrongAnswers(userId, problemId, "KOTLIN").map { it.asDonatable() }

        override fun donatable(userId: String, submissionId: java.util.UUID): DonatableSubmission? =
            submissions.findById(submissionId)
                ?.takeIf { it.userId == userId && it.language == "KOTLIN" && it.verdict == Verdict.WRONG_ANSWER }
                ?.asDonatable()

        private fun Submission.asDonatable() = DonatableSubmission(
            id = id, problemId = problemId, source = submissions.findSource(id).orEmpty(), score = score, createdAt = createdAt,
        )
    }

    /** 기부한 코드와 신고도 그 사람의 것이다 (§11.3). 소스를 비우면 세워진 과녁은 내려간다. */
    @Bean
    fun arenaDonationPersonalArea(repository: CommunityMutantRepository) = object : PersonalData {
        override val area = "arena-donations"
        override fun export(userId: String) = repository.export(userId)
        override fun erase(userId: String) = mapOf("donations" to repository.erase(userId))
    }

    /** 검수는 Admin 의 결정, 세우고 내리는 것은 아레나의 일 (§3.1). */
    @Bean
    fun arenaModeration(community: CommunityMutantService) = object : ArenaModeration {
        override fun queue() = community.queue()

        override fun approve(id: java.util.UUID, reviewer: String, kind: String, note: String?): ArenaModeration.Decision {
            val defect = DefectKind.entries.firstOrNull { it.name == kind }
                ?: return ArenaModeration.Decision.rejected("결함군이 아니다: $kind (${DefectKind.entries.joinToString { it.name }})")
            return community.approve(id, reviewer, defect, note).asDecision()
        }

        override fun reject(id: java.util.UUID, reviewer: String, reason: String) = community.reject(id, reviewer, reason).asDecision()

        override fun resolve(reportId: java.util.UUID, reviewer: String, retire: Boolean, resolution: String) =
            community.resolve(reportId, reviewer, retire, resolution).asDecision()

        private fun CommunityMutantService.ReviewOutcome.asDecision() = when (this) {
            is CommunityMutantService.ReviewOutcome.Decided -> ArenaModeration.Decision.ok(donation)
            is CommunityMutantService.ReviewOutcome.Rejected -> ArenaModeration.Decision.rejected(reason)
        }
    }

    // --- 질문 게시판 (§8.5) ---

    /** 글에 붙일 수 있는 제출 — 글쓴이 자신의, 그 문제의 것 (§3.1 조립 지점). */
    @Bean
    fun anchorableSubmissions(submissions: SubmissionRepository) = object : AnchorableSubmissions {
        override fun lines(userId: String, problemId: String, submissionId: java.util.UUID): List<String>? =
            submissions.findById(submissionId)
                ?.takeIf { it.userId == userId && it.problemId == problemId }
                ?.let { submissions.findSource(it.id)?.lines() ?: emptyList() }

        override fun accepted(userId: String, submissionId: java.util.UUID): Boolean =
            submissions.findById(submissionId)?.let { it.userId == userId && it.verdict == Verdict.ACCEPTED } == true
    }

    /** 기여 점수의 한 항 — 아레나에 세워진 기부 (§8.5 평판). */
    @Bean
    fun approvedDonations(repository: CommunityMutantRepository) = ApprovedDonations { userId -> repository.approvedCount(userId) }

    /** 글에 붙은 리플레이는 그 글을 볼 수 있는 사람이 연다 — 제출 도메인이 게시판에 묻는다 (§3.1). */
    @Bean
    fun sharedSubmissions(discussion: DiscussionService) =
        SharedSubmissions { readerId, submissionId -> discussion.sharedWith(readerId, submissionId) }

    /** 쓴 글과 신고도 그 사람의 것이다 (§11.3). 본문과 코드 구간을 비우고 글은 남긴다. */
    @Bean
    fun discussionPersonalArea(repository: DiscussionRepository) = object : PersonalData {
        override val area = "discussions"
        override fun export(userId: String) = repository.export(userId)
        override fun erase(userId: String) = mapOf("posts" to repository.erase(userId))
    }

    /** 검수는 Admin 의 결정, 내리는 것은 게시판의 일 (§3.1). */
    @Bean
    fun discussionModeration(discussion: DiscussionService) = object : DiscussionModeration {
        override fun queue() = discussion.queue()

        override fun resolve(reportId: java.util.UUID, reviewer: String, hide: Boolean, resolution: String) =
            when (val outcome = discussion.resolve(reportId, reviewer, hide, resolution)) {
                is DiscussionService.ReviewOutcome.Decided -> ArenaModeration.Decision.ok(outcome.post)
                is DiscussionService.ReviewOutcome.Rejected -> ArenaModeration.Decision.rejected(outcome.reason)
            }
    }

    // --- 유사도 신호 (§11.4) ---

    /** 검수자가 두 소스를 나란히 볼 때만 제출 도메인에 묻는다 (§3.1). */
    @Bean
    fun submissionSources(submissions: SubmissionRepository) = SubmissionSources { id -> submissions.findSource(id) }

    /** 지문은 소스와 함께 가고, 신호는 상대방의 기록이라 이름만 지운다 (§11.3). */
    @Bean
    fun integrityPersonalArea(repository: IntegrityRepository) = object : PersonalData {
        override val area = "integrity"
        override fun export(userId: String) = repository.export(userId)
        override fun erase(userId: String) = mapOf("fingerprints" to repository.erase(userId))
    }

    @Bean
    fun integrityModeration(integrity: IntegrityService) = object : IntegrityModeration {
        override fun queue() = integrity.queue()

        override fun resolve(id: java.util.UUID, reviewer: String, confirmed: Boolean, note: String?) =
            when (val outcome = integrity.resolve(id, reviewer, confirmed, note)) {
                is IntegrityService.ReviewOutcome.Decided -> ArenaModeration.Decision.ok(outcome.flag)
                is IntegrityService.ReviewOutcome.Rejected -> ArenaModeration.Decision.rejected(outcome.reason)
            }
    }

    /**
     * 공유된 풀이를 실험실에 세운다 (§8.5 "실행 가능한 인터랙티브 해설", §3.1 조립 지점).
     *
     * 게시판은 풀이 글을 알고, 실험실은 이름·언어·소스만 안다. 언어는 붙은 제출의 것이고,
     * 그것을 아는 곳은 제출 도메인이라 여기서 잇는다. 같은 제목이 둘일 수 있어 id 여덟 자를 붙인다.
     */
    @Bean
    fun sharedApproaches(discussion: DiscussionRepository, submissions: SubmissionRepository) = SharedApproaches { problemId ->
        discussion.solutions(problemId, SHARED_APPROACH_LIMIT).mapNotNull { post ->
            val anchor = post.anchor ?: return@mapNotNull null
            val source = anchor.excerpt ?: return@mapNotNull null
            val language = submissions.findById(anchor.submissionId)?.language ?: return@mapNotNull null
            SharedApproach("${post.title} (${post.id.toString().take(8)})", language, source)
        }
    }

    // --- 제재 (§8.5, §10.4) ---

    /** 제재도 그 사람의 기록이다 (§11.3). 운영 기록이라 남기되 이름은 지우고, 이의의 글은 지운다. */
    @Bean
    fun sanctionPersonalArea(repository: SanctionRepository) = object : PersonalData {
        override val area = "sanctions"
        override fun export(userId: String) = repository.export(userId)
        override fun erase(userId: String) = mapOf("sanctions" to repository.erase(userId))
    }

    /** 계정에 닿는 결정은 Admin 이 넘기고 Identity 가 적는다 (§3.1). */
    @Bean
    fun sanctionModeration(service: SanctionService) = object : SanctionModeration {
        override fun issue(userId: String, kind: String, reason: String, evidence: String, days: Int?, issuedBy: String): ArenaModeration.Decision {
            val parsed = SanctionKind.entries.firstOrNull { it.name == kind }
                ?: return ArenaModeration.Decision.rejected("제재 종류가 아니다: $kind (${SanctionKind.entries.joinToString { it.name }})")
            return service.issue(userId, parsed, reason, evidence, days, issuedBy).asDecision()
        }

        override fun lift(id: java.util.UUID, by: String) = service.lift(id, by).asDecision()
        override fun appeals() = service.openAppeals()
        override fun resolveAppeal(id: java.util.UUID, by: String, uphold: Boolean, note: String?) = service.resolveAppeal(id, by, uphold, note).asDecision()
        override fun history(userId: String) = service.history(userId)

        private fun SanctionService.Outcome.asDecision() = when (this) {
            is SanctionService.Outcome.Decided -> ArenaModeration.Decision.ok(sanction)
            is SanctionService.Outcome.Rejected -> ArenaModeration.Decision.rejected(reason)
        }
    }

    // --- 대회 (§8.4) ---

    /** 대회에는 공개된 문제만 건다 — 문제 도메인에 묻는다 (§3.1). */
    @Bean
    fun contestProblems(publish: PublishService) = ContestProblems { problemId -> problemId in publish.publishedProblemIds() }

    /** 순위표의 이름은 그 사람의 것이다 (§11.3). 지우면 이름 없는 줄이 된다. */
    @Bean
    fun contestPersonalArea(repository: ContestRepository) = object : PersonalData {
        override val area = "contests"
        override fun export(userId: String) = repository.export(userId)
        override fun erase(userId: String) = mapOf("entries" to repository.erase(userId))
    }

    @Bean
    fun contestAdministration(contests: ContestService) = object : ContestAdministration {
        override fun create(createdBy: String, kind: String, title: String, problemIds: List<String>, startsAt: java.time.Instant, endsAt: java.time.Instant, rated: Boolean): ArenaModeration.Decision {
            val parsed = dev.codedrill.controlplane.contest.Contest.Kind.entries.firstOrNull { it.name == kind }
                ?: return ArenaModeration.Decision.rejected("대회 종류가 아니다: $kind (CONTEST, HACK)")
            return contests.create(createdBy, parsed, title, problemIds, startsAt, endsAt, rated).asDecision()
        }

        override fun publish(id: java.util.UUID, actor: String) = contests.publish(id, actor).asDecision()

        private fun ContestService.AdminOutcome.asDecision() = when (this) {
            is ContestService.AdminOutcome.Decided -> ArenaModeration.Decision.ok(contest)
            is ContestService.AdminOutcome.Rejected -> ArenaModeration.Decision.rejected(reason)
        }
    }

    /** 역량 증거도 사용자의 기록이다 (§11.3). */
    @Bean
    fun competencyPersonalArea(repository: EvidenceRepository) = object : PersonalData {
        override val area = "competency"
        override fun export(userId: String) = mapOf("evidence" to repository.export(userId))
        override fun erase(userId: String) = mapOf("evidence" to repository.erase(userId))
    }

    /**
     * 재채점과 제출 도메인을 잇는 어댑터 두 개 (§3.1 조립 지점).
     *
     * 두 모듈은 서로를 모른다. Admin 은 "무엇을 다시 돌릴지"를 정하고, 제출 도메인은
     * "제출을 다시 큐에 올리는" 법을 안다. 둘을 아는 곳은 여기 하나뿐이다.
     *
     * 타입이 양쪽에 하나씩 있어 여기서 옮겨 담는다. 공유 타입을 만들면 편하지만, 그
     * 타입을 놓을 곳이 없어 결국 한 모듈이 다른 모듈을 참조하게 된다.
     *
     * **두 방향의 의존이 순환을 만든다.** 제출은 판정을 확정할 때 재채점을 묻고,
     * 재채점은 실행할 때 제출을 다시 건다. 이것은 두 도메인의 실제 관계이지 설계
     * 실수가 아니므로, 없애는 대신 조립 지점에서 지연 해석으로 흡수한다 — 순환을
     * 도메인 모듈로 밀어 넣으면 그때부터는 진짜 결합이 된다.
     */
    @Bean
    fun judgedSubmissions(submissions: ObjectProvider<SubmissionService>) = object : JudgedSubmissions {
        override fun completedFor(problemId: String) =
            submissions.getObject().completedFor(problemId)

        override fun completed(submissionId: UUID) = submissions.getObject().completed(submissionId)
        override fun requeue(ids: List<UUID>) = submissions.getObject().requeue(ids)
    }

    @Bean
    fun rejudgeContext(rejudge: RejudgeService) = object : RejudgeContext {
        override fun pendingFor(submissionId: UUID) =
            rejudge.pendingFor(submissionId)?.let {
                RejudgeContext.Pending(jobId = it.jobId, dryRun = it.dryRun)
            }

        override fun judged(outcome: RejudgeContext.Outcome) = rejudge.judged(
            RejudgeResult(
                submissionId = outcome.submissionId,
                jobId = outcome.jobId,
                applied = outcome.applied,
                previousVerdict = outcome.previousVerdict,
                previousScore = outcome.previousScore,
                verdict = outcome.verdict,
                score = outcome.score,
            ),
        )
    }

    /**
     * 아웃박스 이벤트를 어느 큐로, 어떤 타입으로 보낼지 (§3.3).
     *
     * 새 이벤트 타입을 추가하면 여기에도 등록해야 발행된다. 등록되지 않은 타입은
     * 퍼블리셔가 경고를 남기고 건너뛴다.
     */
    @Bean
    fun outboxRoutes(mapper: ObjectMapper) = object : OutboxRoutes {
        override fun routeFor(type: String): OutboxRoute? = when (type) {
            "SubmissionQueued" -> OutboxRoute(JudgeQueues.SUBMISSIONS) {
                mapper.readValue<SubmissionQueued>(it)
            }
            // 시험 실행은 오케스트레이터를 거치지 않으므로 payload 가 곧 실행 요청이다.
            // 판정 큐와 다른 큐로 간다 — 이유는 JudgeQueues.TRIALS 에 있다.
            TrialService.TRIAL_EVENT -> OutboxRoute(JudgeQueues.TRIALS) {
                mapper.readValue<ExecutionRequest>(it)
            }
            // 아레나도 판정이 아니다 (§8.3).
            ArenaService.ARENA_EVENT -> OutboxRoute(JudgeQueues.ARENA) { mapper.readValue<ArenaRequest>(it) }
            // 실험실도 판정이 아니다 (§6.4~6.6).
            LabService.LAB_EVENT -> OutboxRoute(JudgeQueues.LABS) { mapper.readValue<LabRequest>(it) }
            // 반례 축소도 오케스트레이터를 거치지 않는다. 판정이 아니라 판정 뒤의
            // 진단이며, 잃어버리면 사용자가 다시 누른다 (§6.3).
            CounterexampleService.SHRINK_EVENT -> OutboxRoute(JudgeQueues.SHRINKS) {
                mapper.readValue<ShrinkRequest>(it)
            }
            // 참조 트레이스도 오케스트레이터를 거치지 않는다. 판정이 아니므로 임대도
            // fencing 도 없고, 잃어버리면 다음 제출이 다시 건다 (FR-805).
            DivergenceService.REFERENCE_TRACE_EVENT -> OutboxRoute(JudgeQueues.REFERENCE_TRACES) {
                mapper.readValue<ExecutionRequest>(it)
            }
            // 변이 평가도 오케스트레이터를 거치지 않는다. 정답 한 번 + 오답 N 번이 한
            // 봉투에 들어 있어, 짝지을 것도 잃어버렸는지 셀 것도 없다.
            MutationService.MUTATION_EVENT -> OutboxRoute(JudgeQueues.MUTATIONS) {
                mapper.readValue<MutationRequest>(it)
            }
            // 프로젝트형 제출 (11단계). 오케스트레이터를 거친다 — 임대와 fencing 이 있는 판정이다.
            ProjectService.PROJECT_EVENT -> OutboxRoute(JudgeQueues.PROJECT_SUBMISSIONS) {
                mapper.readValue<ProjectQueued>(it)
            }
            else -> null
        }
    }
}

/**
 * 도움이 급한 순서 (FR-802 "약한 역량").
 *
 * DEVELOPING 이 먼저다. 아직 재지 않은 역량은 **못하는 것이 아니라 모르는 것**이라,
 * 확인된 약점보다 뒤에 선다. STRONG 은 목록에 없다 — 여기 있으면 잘하는 것에 힌트가
 * 붙는다.
 */
private val NEEDS_HELP = listOf(
    MasteryLevel.DEVELOPING,
    MasteryLevel.UNMEASURED,
    MasteryLevel.PROFICIENT,
)

/**
 * 정답 전에 해설을 연 것의 도움 단계 (FR-214).
 *
 * 힌트 사다리의 마지막 칸과 같다 — 방법을 본 것이다. 코칭의 3단계가 "방법"이므로 같은
 * 값을 준다. 더 낮추면 힌트를 세 번 누른 사람보다 해설을 통째로 본 사람이 유리해진다.
 */
private const val EDITORIAL_HELP = 3

/** 실험실 목록에 올리는 공유 풀이의 수. 도움됐다 순은 아직 없다 — 최근 것부터 (§8.5). */
private const val SHARED_APPROACH_LIMIT = 10
