package tests

import cache.TtlCache
import codedrill.*

/** 공개 테스트. 채점은 이것과 숨은 테스트를 함께 돌린다. 하네스의 단언을 쓴다. */
class PublicCacheTest {
    private var now = 0L
    private fun cache(capacity: Int = 2, ttl: Long = 100) = TtlCache<String, Int>(capacity, ttl) { now }

    fun testPutAndGet() {
        val cache = cache()
        cache.put("a", 1)
        assertEquals(1, cache.get("a"))
        assertNull(cache.get("missing"))
    }

    fun testEvictsLeastRecentlyUsedWhenFull() {
        val cache = cache(capacity = 2)
        cache.put("a", 1)
        cache.put("b", 2)
        cache.put("c", 3)
        assertNull(cache.get("a"))
        assertEquals(listOf("b", "c"), cache.keys())
    }

    fun testExpiresAfterTtl() {
        val cache = cache(ttl = 100)
        cache.put("a", 1)
        now = 99
        assertEquals(1, cache.get("a"))
        now = 100
        assertNull(cache.get("a"))
    }
}
