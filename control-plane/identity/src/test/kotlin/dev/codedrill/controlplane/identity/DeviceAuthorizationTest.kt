package dev.codedrill.controlplane.identity

import dev.codedrill.controlplane.identity.DeviceAuthorization.Answer
import java.time.Duration
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 기기 승인의 규칙 (RFC 8628). 저장소를 거치는 흐름은 스모크가 개발 스택에서 끝까지 돌린다 — 여기는 판단만.
 */
class DeviceAuthorizationTest {

    private val now = Instant.parse("2026-10-07T12:00:00Z")
    private val later = now.plusSeconds(600)
    private val interval = Duration.ofSeconds(5)

    @Test
    fun `사용자 코드는 헷갈리는 글자 없이 넷-넷이고, 사람이 친 것을 맞춰 준다`() {
        repeat(200) {
            val code = DeviceAuthorization.userCode()
            assertTrue(code.matches(Regex("[BCDFGHJKLMNPQRSTVWXZ]{4}-[BCDFGHJKLMNPQRSTVWXZ]{4}")), code)
        }
        assertEquals("WQXR-KDPB", DeviceAuthorization.normalize(" wqxr kdpb "))
        assertEquals("WQXR-KDPB", DeviceAuthorization.normalize("WQXRKDPB"))
        assertNull(DeviceAuthorization.normalize("WQXR-KDP"), "일곱 글자")
        assertNull(DeviceAuthorization.normalize("WQXR-KDPA"), "모음은 쓰지 않는다")
    }

    @Test
    fun `기다리는 중이면 기다리라고 하고, 간격보다 빨리 물으면 늦추라고 한다`() {
        assertEquals(Answer.Pending, DeviceAuthorization.answer("PENDING", later, null, now, interval))
        assertEquals(Answer.SlowDown, DeviceAuthorization.answer("PENDING", later, now.minusSeconds(2), now, interval))
        assertEquals(Answer.Pending, DeviceAuthorization.answer("PENDING", later, now.minusSeconds(5), now, interval))
    }

    @Test
    fun `승인됐으면 세션을 내고, 거절됐으면 거절이라고 한다`() {
        assertEquals(Answer.Issue, DeviceAuthorization.answer("APPROVED", later, now.minusSeconds(1), now, interval))
        assertEquals(Answer.Denied, DeviceAuthorization.answer("DENIED", later, null, now, interval))
    }

    @Test
    fun `만료가 먼저다 — 승인된 뒤라도 시한을 넘겼거나 이미 썼으면 받지 못한다`() {
        assertEquals(Answer.Expired, DeviceAuthorization.answer("APPROVED", now, null, now, interval))
        assertEquals(Answer.Expired, DeviceAuthorization.answer("CONSUMED", later, null, now, interval))
    }

    @Test
    fun `기기 세션은 프로젝트형과 나는 누구인가·로그아웃만 연다`() {
        assertTrue(DeviceAuthorization.allows("GET", "/api/v1/projects/job-queue/kit"))
        assertTrue(DeviceAuthorization.allows("POST", "/api/v1/projects/job-queue/submissions"))
        assertTrue(DeviceAuthorization.allows("PUT", "/api/v1/projects/job-queue/draft"))
        assertTrue(DeviceAuthorization.allows("GET", "/api/v1/auth/me"))
        assertTrue(DeviceAuthorization.allows("POST", "/api/v1/auth/logout"))
        // 계정을 바꾸는 일, 다른 기기를 승인하는 일, 알고리즘 제출은 못 한다
        assertFalse(DeviceAuthorization.allows("PATCH", "/api/v1/auth/me"))
        assertFalse(DeviceAuthorization.allows("POST", "/api/v1/auth/me/password"))
        assertFalse(DeviceAuthorization.allows("POST", "/api/v1/auth/device/requests/WQXR-KDPB"))
        assertFalse(DeviceAuthorization.allows("POST", "/api/v1/submissions"))
        assertFalse(DeviceAuthorization.allows("GET", "/api/v1/projectsX"))
    }
}
