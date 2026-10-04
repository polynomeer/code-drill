package dev.codedrill.controlplane.notification

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import java.sql.Timestamp
import java.time.Clock
import java.time.Duration
import java.time.Instant

@Service
class NotificationService(
    private val sources: NotificationSources,
    private val repository: NotificationRepository,
    private val clock: Clock = Clock.systemUTC(),
) {

    /**
     * 최근 [WINDOW] 의 알림, 새것부터 [LIMIT] 개. "안 읽음"은 마지막으로 읽은 때보다 뒤에 알릴 만해진 것.
     * 한 번도 읽지 않았으면 전부 안 읽음이다.
     */
    fun feed(userId: String): Feed {
        val now = clock.instant()
        val readUntil = repository.readUntil(userId)
        val items = sources.since(userId, now.minus(WINDOW), now)
            .filter { !it.at.isAfter(now) }
            .distinctBy { it.id }
            .sortedWith(compareByDescending<Notification> { it.at }.thenBy { it.id })
            .take(LIMIT)
            .map { FeedItem(it.id, it.kind, it.title, it.body, it.link, it.at, readUntil == null || it.at.isAfter(readUntil)) }
        return Feed(items, items.count { it.unread })
    }

    /** 지금까지를 읽었다. 그 뒤에 생긴 것만 다시 안 읽음이 된다. */
    fun markRead(userId: String) {
        repository.markRead(userId, clock.instant())
    }

    companion object {
        val WINDOW: Duration = Duration.ofDays(30)
        const val LIMIT = 30
    }
}

@Repository
class NotificationRepository(private val jdbc: JdbcTemplate) {

    fun readUntil(userId: String): Instant? = jdbc.query(
        "SELECT read_until FROM notification_read WHERE user_id = ?",
        { rs, _ -> rs.getTimestamp(1).toInstant() },
        userId,
    ).firstOrNull()

    fun markRead(userId: String, until: Instant): Int = jdbc.update(
        """
        INSERT INTO notification_read (user_id, read_until) VALUES (?, ?)
        ON CONFLICT (user_id) DO UPDATE SET read_until = GREATEST(notification_read.read_until, EXCLUDED.read_until)
        """.trimIndent(),
        userId, Timestamp.from(until),
    )

    fun erase(userId: String): Int = jdbc.update("DELETE FROM notification_read WHERE user_id = ?", userId)
}
