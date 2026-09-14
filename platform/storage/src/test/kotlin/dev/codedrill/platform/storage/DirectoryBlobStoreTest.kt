package dev.codedrill.platform.storage

import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DirectoryBlobStoreTest {

    private val store = DirectoryBlobStore(createTempDirectory("blobs"))

    @Test
    fun `넣은 것을 그대로 돌려주고 없는 것은 null 이다`() {
        assertNull(store.get("bundles/x"))
        assertFalse(store.exists("bundles/x"))

        store.put("bundles/x", byteArrayOf(1, 2, 3))

        assertTrue(store.exists("bundles/x"))
        assertContentEquals(byteArrayOf(1, 2, 3), store.get("bundles/x"))
    }

    @Test
    fun `키로 스토어 밖을 가리킬 수 없다`() {
        assertFailsWith<IllegalArgumentException> { store.put("../escape", byteArrayOf()) }
    }
}
