package dev.codedrill.controlplane.identity

import java.time.Instant
import java.util.UUID

/**
 * 사용자 (기술 설계서 §8.1).
 *
 * 비밀번호 해시는 이 모델에 담지 않는다. 인증에 필요한 곳은 저장소 한 군데뿐이고,
 * 도메인 객체가 들고 다니면 로그·응답 어딘가에 실려 나갈 길이 생긴다 (§11.3).
 */
data class User(
    val id: UUID,
    val email: String,
    val displayName: String,
    val createdAt: Instant,
)

/**
 * 발급된 토큰 한 쌍 (§11.2).
 *
 * 평문 토큰은 **발급하는 이 순간에만** 존재한다. 저장소에는 해시만 남으므로 잃어버리면
 * 다시 볼 수 없고, 그래서 유출된 DB 만으로는 로그인할 수 없다.
 */
data class IssuedSession(
    val accessToken: String,
    val refreshToken: String,
    val accessExpiresAt: Instant,
    val refreshExpiresAt: Instant,
    val user: User,
)
