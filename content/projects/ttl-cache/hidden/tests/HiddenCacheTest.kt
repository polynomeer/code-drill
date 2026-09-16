package tests

import cache.TtlCache
import codedrill.*

/** 숨은 테스트. 사용자에게 나가지 않는다. */
class HiddenCacheTest {
    private var now = 0L
    private fun cache(capacity: Int = 2, ttl: Long = 100) = TtlCache<String, Int>(capacity, ttl) { now }

    fun testGetRefreshesRecency() {
        val cache = cache(capacity = 2)
        cache.put("a", 1)
        cache.put("b", 2)
        cache.get("a")          // a 가 최근이 된다
        cache.put("c", 3)       // 그러니 b 가 나간다
        assertNull(cache.get("b"))
        assertEquals(1, cache.get("a"))  // 다시 최근이 된다
        assertEquals(listOf("c", "a"), cache.keys())
    }

    fun testPutExistingKeyRefreshesTtlAndRecency() {
        val cache = cache(capacity = 2, ttl = 100)
        cache.put("a", 1)
        cache.put("b", 2)
        now = 50
        cache.put("a", 10)      // 만료 시각이 150 이 되고, a 가 최근이 된다
        now = 120
        assertEquals(10, cache.get("a"))
        assertNull(cache.get("b"))
    }

    fun testExpiredEntriesDoNotCountTowardCapacity() {
        val cache = cache(capacity = 2, ttl = 100)
        cache.put("a", 1)       // 만료 100
        now = 50
        cache.put("b", 2)       // 만료 150
        now = 60
        cache.get("a")          // a 가 최근, b 가 가장 오래 쓰지 않은 것
        now = 120               // a 만 만료
        cache.put("c", 3)       // 만료된 a 를 지우면 자리가 있다 — 멀쩡한 b 를 내보내면 안 된다
        assertEquals(listOf("b", "c"), cache.keys())
        assertEquals(2, cache.get("b"))
        assertEquals(2, cache.size)
    }

    fun testExpiryIsInclusiveAtBoundary() {
        val cache = cache(ttl = 10)
        cache.put("a", 1)
        now = 9
        assertEquals(1, cache.size)
        now = 10
        assertEquals(0, cache.size)
        assertNull(cache.get("a"))
    }

    fun testEvictsOldestNotNewest() {
        val cache = cache(capacity = 3)
        cache.put("a", 1); cache.put("b", 2); cache.put("c", 3)
        cache.put("d", 4)
        assertNull(cache.get("a"))
        assertEquals(listOf("b", "c", "d"), cache.keys())
    }

    fun testRemove() {
        val cache = cache()
        cache.put("a", 1)
        assertTrue(cache.remove("a"))
        assertFalse(cache.remove("a"))
        assertNull(cache.get("a"))
        assertEquals(0, cache.size)
    }

    fun testRemoveExpiredStillReportsPresence() {
        val cache = cache(ttl = 10)
        cache.put("a", 1)
        now = 10
        assertTrue(cache.remove("a"))
    }

    fun testKeysOrderIsLeastRecentFirst() {
        val cache = cache(capacity = 3)
        cache.put("a", 1); cache.put("b", 2); cache.put("c", 3)
        cache.get("a")
        assertEquals(listOf("b", "c", "a"), cache.keys())
    }

    fun testRejectsBadParameters() {
        assertThrows<IllegalArgumentException> { TtlCache<String, Int>(0, 10) { 0L } }
        assertThrows<IllegalArgumentException> { TtlCache<String, Int>(1, 0) { 0L } }
    }
}
