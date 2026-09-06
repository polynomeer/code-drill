package dev.codedrill.platform.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CursorTest {

    @Test
    fun `인코딩한 값을 그대로 되돌린다`() {
        val cursor = Cursor.encode("2026-09-06T00:00:00Z", "abc-123")

        assertEquals(listOf("2026-09-06T00:00:00Z", "abc-123"), Cursor.decode(cursor))
    }

    @Test
    fun `깨진 커서는 첫 페이지로 되돌린다`() {
        // 사용자가 손댄 커서 때문에 목록이 500 을 내면 안 된다.
        assertNull(Cursor.decode("!!!not-base64!!!"))
        assertNull(Cursor.decode(""))
        assertNull(Cursor.decode(null))
    }

    @Test
    fun `limit 은 범위를 벗어나지 않는다`() {
        assertEquals(20, Cursor.limitOf(null))
        assertEquals(1, Cursor.limitOf(0))
        assertEquals(Cursor.MAX_LIMIT, Cursor.limitOf(10_000))
        assertEquals(35, Cursor.limitOf(35))
    }
}
