package dev.codedrill.controlplane.admin

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 관리자 신원 (기술 설계서 §11.2, §11.4 API authz).
 *
 * 이 테스트가 고정하는 것은 하나다: **설정하지 않으면 열리지 않는다.**
 */
class AdminIdentityTest {

    @Test
    fun `설정이 비어 있으면 아무도 인증되지 않는다`() {
        val properties = AdminProperties(operators = "")

        assertTrue(properties.byToken.isEmpty())
        assertNull(properties.resolve("아무-토큰"))
    }

    @Test
    fun `토큰으로 이름과 역할을 찾는다`() {
        val properties = AdminProperties(
            operators = "hana:$TOKEN_A:CONTENT_EDITOR;dubi:$TOKEN_B:PUBLISHER,REVIEWER",
        )

        val editor = properties.resolve(TOKEN_A)!!
        assertEquals("hana", editor.name)
        assertEquals(setOf(AdminRole.CONTENT_EDITOR), editor.roles)

        val publisher = properties.resolve(TOKEN_B)!!
        assertEquals(setOf(AdminRole.PUBLISHER, AdminRole.REVIEWER), publisher.roles)
    }

    @Test
    fun `모르는 토큰은 인증되지 않는다`() {
        val properties = AdminProperties(operators = "hana:$TOKEN_A:CONTENT_EDITOR")

        assertNull(properties.resolve(TOKEN_B))
        assertNull(properties.resolve(null))
        assertNull(properties.resolve(""))
    }

    @Test
    fun `짧은 토큰은 기동 시점에 거절한다`() {
        // 나중에 요청이 들어올 때가 아니라 여기서 터져야 한다. 약한 토큰으로 뜬 서비스는
        // 아무 오류도 내지 않으면서 계속 약하다.
        val error = assertFailsWith<IllegalArgumentException> {
            AdminProperties(operators = "hana:짧다:CONTENT_EDITOR").byToken
        }
        assertTrue(error.message!!.contains("토큰"), "사유: ${error.message}")
    }

    @Test
    fun `형식이 어긋난 설정은 조용히 무시하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            AdminProperties(operators = "hana:$TOKEN_A").byToken
        }
        assertFailsWith<IllegalArgumentException> {
            AdminProperties(operators = "hana:$TOKEN_A:이런역할은없다").byToken
        }
    }

    @Test
    fun `공백과 빈 항목은 넘어간다`() {
        val properties = AdminProperties(operators = " hana:$TOKEN_A:CONTENT_EDITOR ; ")

        assertEquals(1, properties.byToken.size)
        assertEquals("hana", properties.resolve(TOKEN_A)?.name)
    }

    private companion object {
        const val TOKEN_A = "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        const val TOKEN_B = "bbbbbbbbbbbbbbbbbbbbbbbbbbbb"
    }
}
