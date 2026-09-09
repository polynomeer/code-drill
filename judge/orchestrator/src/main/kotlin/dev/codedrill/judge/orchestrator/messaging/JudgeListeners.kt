package dev.codedrill.judge.orchestrator.messaging

import dev.codedrill.judge.orchestrator.JudgeCoordinator
import dev.codedrill.judge.protocol.ExecutionHeartbeat
import dev.codedrill.judge.protocol.ExecutionResult
import dev.codedrill.judge.protocol.SubmissionQueued
import dev.codedrill.platform.messaging.JudgeQueues
import org.slf4j.LoggerFactory
import org.springframework.amqp.AmqpRejectAndDontRequeueException
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component

/**
 * 브로커 수신 지점.
 *
 * 작업은 at-least-once 로 전달되므로 두 핸들러 모두 멱등해야 한다. 멱등성은
 * [dev.codedrill.judge.orchestrator.lease.AttemptRegistry] 가 책임진다 (§4.3).
 *
 * **재시도해서 될 실패와 안 될 실패를 가른다** (§10.2). 브로커는 그 둘을 구분하지
 * 못하므로 기본값이 "다시 넣기"이고, 그러면 처리할 수 없는 메시지가 큐 머리에서
 * 무한히 돌며 뒤의 멀쩡한 작업을 전부 막는다 — 실제로 그렇게 채점이 섰다.
 */
@Component
class JudgeListeners(private val coordinator: JudgeCoordinator) {

    private val log = LoggerFactory.getLogger(javaClass)

    @RabbitListener(queues = [JudgeQueues.SUBMISSIONS])
    fun onSubmission(message: SubmissionQueued) = unlessMalformed(message.submissionId) {
        coordinator.onSubmissionQueued(message)
    }

    @RabbitListener(queues = [JudgeQueues.RESULTS])
    fun onResult(result: ExecutionResult) = unlessMalformed(result.submissionId) {
        coordinator.onExecutionResult(result, correlationId = result.executionId)
    }

    @RabbitListener(queues = [JudgeQueues.HEARTBEATS])
    fun onHeartbeat(heartbeat: ExecutionHeartbeat) = coordinator.onHeartbeat(heartbeat)

    /**
     * 메시지가 잘못된 것이면 다시 넣지 않는다.
     *
     * [IllegalArgumentException] 은 "이 메시지로는 할 수 있는 일이 없다"는 뜻으로 쓴다 —
     * 없는 문제 버전을 가리킨다거나, 패키지와 어긋난다거나. 다시 넣어도 결과가 같으므로
     * 브로커에 돌려주는 대신 dead 큐로 보낸다.
     *
     * 나머지 예외는 그대로 던진다. DB 가 잠깐 흔들리거나 브로커가 재연결하는 동안 난
     * 실패는 다시 해 보면 되고, 그 판단까지 여기서 하면 일시적인 장애에 작업을 버린다.
     */
    private inline fun unlessMalformed(submissionId: String, work: () -> Unit) {
        try {
            work()
        } catch (e: IllegalArgumentException) {
            log.error("처리할 수 없는 메시지다. dead 큐로 보낸다: {} — {}", submissionId, e.message)
            throw AmqpRejectAndDontRequeueException(e)
        }
    }
}
