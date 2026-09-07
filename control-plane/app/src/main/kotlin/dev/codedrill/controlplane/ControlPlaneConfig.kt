package dev.codedrill.controlplane

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import dev.codedrill.judge.protocol.SubmissionQueued
import dev.codedrill.platform.messaging.JudgeQueues
import dev.codedrill.controlplane.outbox.OutboxRoute
import dev.codedrill.controlplane.outbox.OutboxRoutes
import dev.codedrill.controlplane.admin.JudgedSubmissions
import dev.codedrill.controlplane.admin.PublishService
import dev.codedrill.controlplane.admin.RejudgeResult
import dev.codedrill.controlplane.admin.RejudgeService
import dev.codedrill.controlplane.problem.PublishedProblems
import dev.codedrill.controlplane.submission.RejudgeContext
import dev.codedrill.controlplane.submission.SubmissionService
import dev.codedrill.platform.problempackage.ProblemPackageLoader
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import java.nio.file.Path
import java.util.UUID

@Configuration
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
            else -> null
        }
    }
}
