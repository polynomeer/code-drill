package dev.codedrill.controlplane.identity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class HandleTest {

    @Test
    fun `소문자로 정규화한다`() {
        assertEquals(Handle.Result.Valid("code_drill-1"), Handle.parse("  Code_Drill-1 "))
    }

    @Test
    fun `주소에 그대로 쓸 수 없는 글자와 길이를 막는다`() {
        for (raw in listOf("ab", "a".repeat(21), "-lead", "한글이름", "has space", "dot.name")) {
            assertIs<Handle.Result.Invalid>(Handle.parse(raw), raw)
        }
    }

    @Test
    fun `화면 경로와 겹치는 이름은 쓸 수 없다`() {
        assertIs<Handle.Result.Invalid>(Handle.parse("me"))
        assertIs<Handle.Result.Invalid>(Handle.parse("ADMIN"))
    }
}
