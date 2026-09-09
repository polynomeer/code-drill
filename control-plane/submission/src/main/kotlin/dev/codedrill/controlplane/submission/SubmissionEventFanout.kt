package dev.codedrill.controlplane.submission

import dev.codedrill.platform.messaging.JudgeQueues
import dev.codedrill.platform.messaging.SubmissionEvent
import org.springframework.amqp.rabbit.annotation.Exchange
import org.springframework.amqp.rabbit.annotation.Queue
import org.springframework.amqp.rabbit.annotation.QueueBinding
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component

/**
 * 팬아웃으로 온 제출 이벤트를 이 인스턴스의 SSE 구독자에게 흘린다 (기술 설계서 §9.1).
 *
 * **큐에 이름을 주지 않는다.** 익명·배타·자동 삭제 큐라 인스턴스가 사라지면 함께
 * 사라진다. 이름 있는 큐를 두면 죽은 인스턴스 몫의 큐에 이벤트가 영원히 쌓이고,
 * 브로커 디스크가 그 다음 사고가 된다.
 *
 * 이벤트를 놓쳐도 된다. 진실의 원천은 DB 조회이며, 클라이언트는 재조회로 수렴한다.
 * 그래서 durable 큐도, 재시도도 두지 않는다.
 */
@Component
class SubmissionEventFanout(private val events: SubmissionEventStream) {

    @RabbitListener(
        bindings = [
            QueueBinding(
                value = Queue(exclusive = "true", durable = "false", autoDelete = "true"),
                exchange = Exchange(name = JudgeQueues.SUBMISSION_EVENTS, type = "fanout", durable = "true"),
            ),
        ],
    )
    fun onEvent(message: SubmissionEvent) {
        val payload = message.payload["data"] ?: return
        events.deliver(message.submissionId, message.event, payload)
    }
}
