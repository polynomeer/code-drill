package dev.codedrill.controlplane.outbox

import dev.codedrill.platform.messaging.JudgeQueues

import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * 아웃박스 퍼블리셔 (기술 설계서 §3.2, §12.2).
 *
 * 커밋된 이벤트를 브로커로 옮긴다. 발행과 표시 사이에 죽으면 같은 이벤트가 다시
 * 발행되므로 **at-least-once** 다. 중복은 소비자가 멱등하게 흡수한다.
 *
 * `FOR UPDATE SKIP LOCKED` 로 행을 잡아, replica 가 여러 개여도 같은 이벤트를 두
 * 인스턴스가 동시에 밀지 않는다.
 *
 * 제어 영역에만 둔다. 실행 영역은 Control DB 에 접근하지 않으므로, 이 클래스도 이것이
 * 끌고 오는 JDBC 도 그쪽 배포 단위에 들어가서는 안 된다 (§2.3).
 */
@Component
class OutboxPublisher(
    private val jdbc: JdbcTemplate,
    private val rabbit: RabbitTemplate,
    private val routes: OutboxRoutes,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelayString = "\${codedrill.outbox.poll-interval-ms:200}")
    @Transactional
    fun publishPending() {
        val pending = jdbc.query(
            """
            SELECT id, type, payload FROM outbox_event
             WHERE published_at IS NULL
             ORDER BY sequence_no
             LIMIT ?
             FOR UPDATE SKIP LOCKED
            """.trimIndent(),
            { rs, _ -> Pending(rs.getObject(1, UUID::class.java), rs.getString(2), rs.getString(3)) },
            BATCH_SIZE,
        )

        for (event in pending) {
            val route = routes.routeFor(event.type)
            if (route == null) {
                log.warn("라우팅을 모르는 아웃박스 이벤트라 건너뛴다: {}", event.type)
                continue
            }
            check(route.queue in JudgeQueues.all) { "알 수 없는 큐: ${route.queue}" }
            rabbit.convertAndSend(route.queue, route.decode(event.payload))
            jdbc.update("UPDATE outbox_event SET published_at = now() WHERE id = ?", event.id)
        }
    }

    private data class Pending(val id: UUID, val type: String, val payload: String)

    private companion object {
        const val BATCH_SIZE = 100
    }
}

/**
 * 이벤트 타입 → 큐와 역직렬화 규칙.
 *
 * 아웃박스는 페이로드를 문자열로 들고 있으므로, 어떤 타입으로 되살릴지는 발행 시점에
 * 정해야 한다. 이 매핑을 한 곳에 모아 두면 새 이벤트를 추가할 때 빠뜨리기 어렵다.
 */
interface OutboxRoutes {
    fun routeFor(type: String): OutboxRoute?
}

data class OutboxRoute(val queue: String, val decode: (String) -> Any)
