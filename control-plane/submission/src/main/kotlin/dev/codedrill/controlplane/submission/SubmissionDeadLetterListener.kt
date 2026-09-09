package dev.codedrill.controlplane.submission

import dev.codedrill.judge.protocol.JudgeCompleted
import dev.codedrill.judge.protocol.SubmissionQueued
import dev.codedrill.judge.protocol.Verdict
import dev.codedrill.platform.messaging.JudgeQueues
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * 채점에 실려 가지 못한 제출을 끝맺는다 (기술 설계서 §4.4, §10.2).
 *
 * 배달 한도를 넘긴 메시지는 브로커가 여기로 치운다. 치우는 것만으로 큐는 다시 흐르지만,
 * **그 제출은 QUEUED 인 채로 영원히 남는다** — 사용자는 채점이 안 온다고만 보고, 왜인지
 * 알 길이 없다.
 *
 * 그래서 SYSTEM_ERROR 로 끝맺는다. §4.4 가 시스템 오류를 판정과 별개의 축으로 둔 이유가
 * 이것이다 — **사용자 코드가 틀린 것이 아니라 우리가 처리하지 못한 것**이고, 그 구분이
 * 남아야 재채점 대상을 고를 수 있다.
 *
 * 실행 요청과 결과가 죽는 경우는 여기서 다루지 않는다. 그쪽은 임대가 만료되면
 * 오케스트레이터가 같은 결론에 도달한다 (§4.3). 제출 큐만 기댈 곳이 없다.
 */
@Component
class SubmissionDeadLetterListener(
    private val service: SubmissionService,
    private val metrics: SubmissionMetrics,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @RabbitListener(queues = [JudgeQueues.SUBMISSIONS_DEAD])
    fun onDeadSubmission(message: SubmissionQueued) {
        // 무엇이 왜 실패했는지는 브로커의 dead 큐에 원본이 남아 있다. 여기서는 그 사실을
        // 제출에 반영하는 것까지만 한다.
        log.error(
            "채점 큐에서 치워진 제출이다. SYSTEM_ERROR 로 끝맺는다: {} ({}@{})",
            message.submissionId, message.problemId, message.problemVersion,
        )

        // 판정 카운터에도 싣는다. 여기서 빠뜨리면 §13.4 의 SYSTEM_ERROR 경보가
        // **가장 조용히 실패하는 경로에만** 눈을 감는다.
        metrics.completed(
            language = message.language.name,
            verdict = Verdict.SYSTEM_ERROR,
            waited = Duration.ZERO,
            propagation = Duration.ZERO,
        )

        service.complete(
            JudgeCompleted(
                submissionId = message.submissionId,
                // 실행이 시작된 적이 없다. 빈 값 대신 사실을 적는다.
                executionId = "dead-letter",
                correlationId = message.correlationId,
                verdict = Verdict.SYSTEM_ERROR,
                score = 0,
                compileLog = null,
                groups = emptyList(),
            ),
        )
    }
}
