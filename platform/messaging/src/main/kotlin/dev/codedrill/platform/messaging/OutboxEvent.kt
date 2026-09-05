package dev.codedrill.platform.messaging

import java.time.Instant
import java.util.UUID

/**
 * 트랜잭셔널 아웃박스 레코드 (기술 설계서 §3.2, §8.1).
 *
 * 상태를 바꾸는 트랜잭션은 도메인 행과 이 레코드를 **함께** 커밋한다. 발행은 커밋 이후
 * 별도 퍼블리셔가 수행하므로, 브로커 장애 중에도 이벤트가 유실되지 않고 축적됐다가
 * 복구 시 발행된다 (§12.2).
 */
data class OutboxEvent(
    val id: UUID,
    val aggregate: String,
    val aggregateId: String,
    val type: String,
    val payload: String,
    val occurredAt: Instant,
    val publishedAt: Instant? = null,
) {
    val published: Boolean get() = publishedAt != null
}
