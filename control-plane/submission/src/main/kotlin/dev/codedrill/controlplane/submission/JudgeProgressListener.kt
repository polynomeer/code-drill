package dev.codedrill.controlplane.submission

import com.fasterxml.jackson.databind.ObjectMapper
import dev.codedrill.judge.protocol.JudgeCompleted
import dev.codedrill.judge.protocol.JudgeProgressed
import dev.codedrill.controlplane.submission.trace.TraceRepository
import dev.codedrill.judge.protocol.TraceReady
import dev.codedrill.platform.messaging.JudgeQueues
import dev.codedrill.platform.observability.CorrelationIds
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitHandler
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * 실행 영역의 진행·종료 알림을 제출 상태에 반영한다 (기술 설계서 §4.1 8단계).
 *
 * 두 핸들러 모두 멱등하다. 같은 종료 알림이 다시 와도 이미 COMPLETED 인 행은 바뀌지
 * 않으며, SSE 도 다시 쏘지 않는다.
 */
@Component
@RabbitListener(queues = [JudgeQueues.PROGRESS])
class JudgeProgressListener(
    private val service: SubmissionService,
    private val events: SubmissionEventStream,
    private val repository: SubmissionRepository,
    private val traces: TraceRepository,
    private val json: ObjectMapper,
    private val metrics: SubmissionMetrics,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @RabbitHandler
    fun onProgress(message: JudgeProgressed) {
        val id = UUID.fromString(message.submissionId)
        val current = repository.findById(id) ?: return
        val next = message.status.toSubmissionStatus()

        if (!current.status.canTransitionTo(next)) {
            // 순서가 뒤바뀐 진행 알림이다. 상태를 되돌리지 않고 무시한다.
            log.debug("건너뛴 진행 알림: {} → {}", current.status, next)
            return
        }
        repository.transition(id, current.status, next, current.version)
        events.publish(message.submissionId, "status", mapOf("status" to next.name))
    }

    @RabbitHandler
    fun onCompleted(message: JudgeCompleted) {
        val receivedAt = Instant.now()
        val changed = service.complete(message)
        if (!changed) {
            log.atInfo()
                .addKeyValue(CorrelationIds.SUBMISSION_ID, message.submissionId)
                .log("이미 종료된 제출이다. no-op")
            return
        }

        log.atInfo()
            .addKeyValue(CorrelationIds.SUBMISSION_ID, message.submissionId)
            .addKeyValue(CorrelationIds.EXECUTION_ID, message.executionId)
            .log("판정 완료: ${message.verdict} (${message.score}점)")

        val submission = service.find(UUID.fromString(message.submissionId)) ?: return
        events.publish(message.submissionId, "completed", SubmissionResponse.of(submission, json))
        // 트레이스가 뒤따라올 수 있으므로 스트림을 여기서 닫지 않는다.

        // 두 구간을 따로 잰다. 사용자가 기다린 전체 시간(제출→종료)과, 종료를 알고 나서
        // 화면에 닿기까지 걸린 시간(§12.1 Verdict propagation)은 원인이 다른 지연이다.
        metrics.completed(
            language = submission.language,
            verdict = message.verdict,
            waited = Duration.between(submission.createdAt, receivedAt),
            propagation = Duration.between(receivedAt, Instant.now()),
        )
    }

    /**
     * 트레이스 도착.
     *
     * 판정과 독립이므로 제출 상태를 건드리지 않는다. 트레이스가 영영 오지 않아도
     * 사용자는 판정 결과를 온전히 본다 (§12.2 장애 격리).
     */
    @RabbitHandler
    fun onTraceReady(message: TraceReady) {
        traces.save(message.manifest, message.chunks)
        // SSE 로는 목차만 알린다. 청크는 클라이언트가 필요한 위치만 내려받는다 (§7.5).
        events.publish(message.submissionId, "trace", message.manifest)
        events.close(message.submissionId)
    }
}

private fun dev.codedrill.judge.protocol.JudgeStatus.toSubmissionStatus() = when (this) {
    dev.codedrill.judge.protocol.JudgeStatus.LEASED -> SubmissionStatus.LEASED
    dev.codedrill.judge.protocol.JudgeStatus.COMPILING -> SubmissionStatus.COMPILING
    dev.codedrill.judge.protocol.JudgeStatus.RUNNING -> SubmissionStatus.RUNNING
    dev.codedrill.judge.protocol.JudgeStatus.AGGREGATING -> SubmissionStatus.AGGREGATING
}
