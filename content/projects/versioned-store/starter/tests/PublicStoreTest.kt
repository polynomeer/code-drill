package tests

import codedrill.*
import store.*

/** 공개 테스트. 채점은 이것과 숨은 테스트를 함께 돌린다. 하네스의 단언을 쓴다. */
class PublicStoreTest {

    fun testPutBumpsVersionAndGetReads() {
        val store = VersionedStore()
        assertEquals(0, store.version)
        assertEquals(1, store.put("a", "1"))
        assertEquals(2, store.put("b", "2"))
        assertEquals("1", store.get("a"))
        assertNull(store.get("zzz"))
        assertEquals(listOf("a", "b"), store.keys())
    }

    fun testGetAtReadsHistory() {
        val store = VersionedStore()
        store.put("a", "1")   // v1
        store.put("a", "2")   // v2
        assertEquals("1", store.getAt("a", 1))
        assertEquals("2", store.getAt("a", 2))
        assertNull(store.getAt("a", 0))
    }

    fun testDeleteRemovesFromCurrent() {
        val store = VersionedStore()
        store.put("a", "1")
        assertEquals(2, store.delete("a"))
        assertNull(store.get("a"))
        assertThrows<NoSuchKeyException> { store.delete("a") }
    }
}
