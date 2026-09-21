package tests

import codedrill.*
import store.*

/** 숨은 테스트. 사용자에게 나가지 않는다. */
class HiddenStoreTest {

    fun testSameValueIsStillAWrite() {
        val store = VersionedStore()
        store.put("a", "1")
        assertEquals(2, store.put("a", "1"))
        assertEquals(2, store.version)
    }

    fun testDeleteKeepsHistoryReadable() {
        val store = VersionedStore()
        store.put("a", "1")   // v1
        store.delete("a")     // v2
        assertNull(store.get("a"))
        assertEquals("1", store.getAt("a", 1))
        assertNull(store.getAt("a", 2))
        assertEquals(listOf<String>(), store.keys())
    }

    fun testGetAtUsesLatestWriteAtOrBefore() {
        val store = VersionedStore()
        store.put("a", "1")   // v1
        store.put("b", "x")   // v2
        store.put("b", "y")   // v3
        store.put("a", "2")   // v4
        assertEquals("1", store.getAt("a", 3))
        assertEquals("1", store.getAt("a", 2))
        assertEquals("x", store.getAt("b", 2))
        assertEquals("2", store.getAt("a", 4))
        assertNull(store.getAt("b", 1))
    }

    fun testGetAtRange() {
        val store = VersionedStore()
        store.put("a", "1")
        assertThrows<IllegalArgumentException> { store.getAt("a", -1) }
        assertThrows<IllegalArgumentException> { store.getAt("a", 2) }
        assertNull(store.getAt("missing", 1))
    }

    fun testRollbackIsANewVersionAndKeepsHistory() {
        val store = VersionedStore()
        store.put("a", "1")   // v1
        store.put("b", "2")   // v2
        store.put("a", "3")   // v3
        assertEquals(4, store.rollback(1))
        assertEquals("1", store.get("a"))
        assertNull(store.get("b"))
        assertEquals(listOf("a"), store.keys())
        // 그 사이의 버전은 그대로 읽힌다.
        assertEquals("3", store.getAt("a", 3))
        assertEquals("2", store.getAt("b", 2))
        assertEquals("1", store.getAt("a", 4))
        assertNull(store.getAt("b", 4))
    }

    fun testRollbackToZeroEmptiesAndToNowIsStillAWrite() {
        val store = VersionedStore()
        store.put("a", "1")
        store.put("b", "2")
        assertEquals(3, store.rollback(0))
        assertEquals(listOf<String>(), store.keys())
        assertEquals(4, store.rollback(3))
        assertEquals(listOf<String>(), store.keys())
        assertEquals("2", store.getAt("b", 2))
    }

    fun testRollbackRangeDoesNotBumpVersion() {
        val store = VersionedStore()
        store.put("a", "1")
        assertThrows<IllegalArgumentException> { store.rollback(2) }
        assertThrows<IllegalArgumentException> { store.rollback(-1) }
        assertEquals(1, store.version)
    }

    fun testDeleteMissingDoesNotBumpVersion() {
        val store = VersionedStore()
        store.put("a", "1")
        store.delete("a")
        assertThrows<NoSuchKeyException> { store.delete("a") }
        assertThrows<NoSuchKeyException> { store.delete("never") }
        assertEquals(2, store.version)
    }

    fun testManyVersionsLookup() {
        val store = VersionedStore()
        for (i in 1..5000) store.put("k", i.toString())
        assertEquals("1", store.getAt("k", 1))
        assertEquals("2500", store.getAt("k", 2500))
        assertEquals("5000", store.getAt("k", 5000))
        assertEquals(5000, store.version)
    }
}
