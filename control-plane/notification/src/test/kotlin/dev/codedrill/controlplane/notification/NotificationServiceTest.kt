package dev.codedrill.controlplane.notification

import org.springframework.jdbc.core.JdbcTemplate
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals

class NotificationServiceTest {

    private val now = Instant.parse("2026-10-05T12:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)

    private fun note(id: String, hoursAgo: Long) =
        Notification(id, Notification.Kind.ANSWERED, id, null, "/x", now.minusSeconds(hoursAgo * 3600))

    @Test
    fun `읽은 때보다 뒤에 생긴 것만 안 읽음이고 새것이 위다`() {
        val repository = MemoryRepository(readUntil = now.minusSeconds(5 * 3600))
        val service = NotificationService({ _, _, _ -> listOf(note("old", 10), note("new", 1), note("mid", 3)) }, repository, clock)

        val feed = service.feed("u1")

        assertEquals(listOf("new", "mid", "old"), feed.items.map { it.id })
        assertEquals(listOf(true, true, false), feed.items.map { it.unread })
        assertEquals(2, feed.unread)
    }

    @Test
    fun `한 번도 읽지 않았으면 전부 안 읽음이고, 읽으면 0 이 된다`() {
        val repository = MemoryRepository(readUntil = null)
        val service = NotificationService({ _, _, _ -> listOf(note("a", 2), note("b", 1)) }, repository, clock)

        assertEquals(2, service.feed("u1").unread)
        service.markRead("u1")
        assertEquals(0, service.feed("u1").unread)
    }

    @Test
    fun `아직 알릴 때가 아닌 것과 같은 사실의 겹친 알림은 뺀다`() {
        val future = Notification("later", Notification.Kind.CONTEST_STARTING, "대회", null, "/c", now.plusSeconds(60))
        val service = NotificationService({ _, _, _ -> listOf(note("a", 1), note("a", 1), future) }, MemoryRepository(null), clock)

        assertEquals(listOf("a"), service.feed("u1").items.map { it.id })
    }

    private class MemoryRepository(var readUntil: Instant?) : NotificationRepository(JdbcTemplate()) {
        override fun readUntil(userId: String) = readUntil
        override fun markRead(userId: String, until: Instant): Int {
            readUntil = until
            return 1
        }
    }
}
