package dev.codedrill.controlplane.submission

import org.slf4j.LoggerFactory
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
 * 슬라이스는 단일 인스턴스 메모리에 구독을 들고 있다. replica 가 늘면 구독자가 붙은
 * 인스턴스와 이벤트를 받은 인스턴스가 달라지므로, 팬아웃 exchange 나 Redis pub/sub 로
 * 옮겨야 한다.
 */
@Component
class SubmissionEventStream {

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

    fun publish(submissionId: String, event: String, payload: Any) {
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
