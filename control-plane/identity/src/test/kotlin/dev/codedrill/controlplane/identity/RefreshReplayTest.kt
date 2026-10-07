package dev.codedrill.controlplane.identity

import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** refresh 재사용 탐지는 회전으로 바뀐 토큰에만 — 끊긴 기기가 갱신을 시도해도 다른 세션은 살아 있어야 한다. */
class RefreshReplayTest {

    private fun session(revokedReason: String?) = IdentityRepository.Session(
        id = UUID.randomUUID(),
        userId = UUID.randomUUID(),
        accessExpiresAt = Instant.EPOCH,
        refreshExpiresAt = Instant.EPOCH,
        revokedAt = revokedReason?.let { Instant.EPOCH },
        revokedReason = revokedReason,
    )

    @Test
    fun `회전으로 바뀐 refresh 가 다시 오면 재사용이다`() {
        assertTrue(IdentityService.isReplay(session(IdentityService.ROTATED)))
    }

    @Test
    fun `로그아웃·기기 연결 끊기로 끝난 세션의 refresh 는 낡았을 뿐이다`() {
        assertFalse(IdentityService.isReplay(session("기기 연결 끊기")))
        assertFalse(IdentityService.isReplay(session("로그아웃")))
        assertFalse(IdentityService.isReplay(session(null)))
    }
}
