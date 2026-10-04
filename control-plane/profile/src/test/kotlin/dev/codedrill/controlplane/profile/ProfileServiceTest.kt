package dev.codedrill.controlplane.profile

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProfileServiceTest {

    /** 2026-10-04 00:30 KST — UTC 로는 아직 10월 3일이다. 날짜는 서울 기준이어야 한다. */
    private val clock = Clock.fixed(Instant.parse("2026-10-03T15:30:00Z"), ZoneId.of("UTC"))
    private val today = LocalDate.of(2026, 10, 4)

    @Test
    fun `비공개 프로필은 남에게 없는 것이고 본인에게는 보인다`() {
        val service = ProfileService(FakeSources(public = false), clock)
        assertNull(service.profile(readerId = "someone", handle = "ada"))
        assertNull(service.profile(readerId = null, handle = "ada"))
        val mine = assertNotNull(service.profile(readerId = "u1", handle = "ada"))
        assertTrue(mine.mine)
    }

    @Test
    fun `활동은 서울 기준 오늘까지 1년을 빈 날까지 채운다`() {
        val profile = assertNotNull(ProfileService(FakeSources(public = true), clock).profile(null, "ada"))
        assertEquals(365, profile.activity.size)
        assertEquals(today, profile.activity.last().date)
        assertEquals(2, profile.activity.last().submissions)
        assertEquals(0, profile.activity.first().submissions)
    }

    @Test
    fun `가장 긴 연속은 하루 빈 날을 넘기고 이틀 빈 날에서 끊는다`() {
        val days = listOf(1, 1, 0, 1, 0, 0, 1, 1).mapIndexed { i, n -> Day(today.minusDays(7L - i), n) }
        // 1,1,(빈),1 = 3 — 이틀 빈 날 뒤 1,1 = 2
        assertEquals(3, ProfileService.longest(days))
    }

    private inner class FakeSources(private val public: Boolean) : ProfileSources {
        override fun owner(handle: String) =
            ProfileOwner("u1", "ada", "에이다", public, Instant.parse("2026-01-01T00:00:00Z")).takeIf { handle == "ada" }

        override fun solved(userId: String) = Solved(3, mapOf("EASY" to 2, "HARD" to 1))
        override fun activity(userId: String, since: LocalDate) = mapOf(today to 2, today.minusDays(400) to 9)
        override fun currentStreak(userId: String) = 1
        override fun rating(userId: String): Rating? = null
        override fun solutions(userId: String, limit: Int) = emptyList<SharedSolution>()
        override fun contributorTier(userId: String) = "NEW"
    }
}
