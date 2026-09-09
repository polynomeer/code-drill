package dev.codedrill.controlplane.submission

import dev.codedrill.platform.messaging.JudgeQueues
import dev.codedrill.platform.messaging.SubmissionEvent
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong

/**
 * 제출 상태 SSE 브로드캐스트 (기술 설계서 §9.1).
 *
 * **이 스트림은 진실의 원천이 아니다.** 최종 상태는 DB 조회가 정한다. 여기서 이벤트를
 * 놓쳐도 클라이언트가 재조회로 수렴할 수 있어야 하며, 그래서 구독이 없을 때 이벤트를
 * 버리는 것이 정상 동작이다.
 *
 * **구독은 인스턴스 메모리에 있고, 이벤트는 팬아웃으로 온다.** 사용자가 A 에 붙어
 * 있는데 판정 결과를 B 가 처리하는 일이 replica 가 둘 이상이면 늘 일어나므로,
 * 발행은 브로커의 팬아웃 exchange 로 나가고 모든 인스턴스가 그것을 받아 자기 구독자에게
 * 흘린다 ([JudgeQueues.SUBMISSION_EVENTS]).
 *
 * 발행과 전달을 갈라 둔 이유가 그것이다. [publish] 는 내보내기만 하고, 실제 전달은
 * 팬아웃을 받은 [deliver] 가 한다 — 두 곳에서 전달하면 발행한 인스턴스의 구독자만
 * 이벤트를 두 번 받는다.
 */
@Component
class SubmissionEventStream(private val rabbit: RabbitTemplate) {

    private val log = LoggerFactory.getLogger(javaClass)
    private val subscribers = ConcurrentHashMap<String, CopyOnWriteArrayList<SseEmitter>>()
    private val sequence = AtomicLong()

    fun subscribe(submissionId: String): SseEmitter {
        val emitter = SseEmitter(STREAM_TIMEOUT_MILLIS)
        val list = subscribers.computeIfAbsent(submissionId) { CopyOnWriteArrayList() }
        list += emitter

        emitter.onCompletion { list.remove(emitter) }
        emitter.onTimeout { list.remove(emitter) }
        emitter.onError { list.remove(emitter) }
        return emitter
    }

    /**
     * 이벤트를 모든 인스턴스로 내보낸다.
     *
     * 브로커가 없으면 이 인스턴스에만 흘린다. 개발 중에 브로커를 내려도 SSE 가 죽지
     * 않게 하려는 것이고, 이 스트림이 진실의 원천이 아니라서 할 수 있는 타협이다.
     */
    fun publish(submissionId: String, event: String, payload: Any) {
        try {
            rabbit.convertAndSend(
                JudgeQueues.SUBMISSION_EVENTS, "",
                SubmissionEvent(submissionId, event, mapOf("data" to payload)),
            )
        } catch (e: Exception) {
            log.warn("제출 이벤트를 내보내지 못했다. 이 인스턴스에만 흘린다: {}", e.message)
            deliver(submissionId, event, payload)
        }
    }

    /** 팬아웃으로 받은 이벤트를 이 인스턴스의 구독자에게 흘린다. */
    fun deliver(submissionId: String, event: String, payload: Any) {
        val list = subscribers[submissionId] ?: return
        val id = sequence.incrementAndGet()
        list.forEach { emitter ->
            try {
                emitter.send(SseEmitter.event().id(id.toString()).name(event).data(payload))
            } catch (e: Exception) {
                // 끊긴 구독은 오류가 아니다. 조용히 정리한다.
                log.debug("SSE 구독이 끊겼다: {}", e.message)
                list.remove(emitter)
            }
        }
    }

    fun close(submissionId: String) {
        subscribers.remove(submissionId)?.forEach { runCatching { it.complete() } }
    }

    private companion object {
        const val STREAM_TIMEOUT_MILLIS = 5 * 60 * 1000L
    }
}
