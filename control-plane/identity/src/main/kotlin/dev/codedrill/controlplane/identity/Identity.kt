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
    /** 공개 프로필 주소의 이름. 고르기 전에는 null. */
    val handle: String? = null,
    /** 공개 프로필을 남에게 보이는가. 기본은 비공개다. */
    val profilePublic: Boolean = false,
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

/** 공개 프로필 설정 (docs/ui-overhaul.md §6.7). */
data class ProfileSettings(val handle: String?, val public: Boolean)

sealed interface ProfileOutcome {
    data class Updated(val settings: ProfileSettings) : ProfileOutcome
    data class Invalid(val reason: String) : ProfileOutcome
    data object Taken : ProfileOutcome
}
