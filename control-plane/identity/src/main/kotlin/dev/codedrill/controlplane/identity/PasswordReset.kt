package dev.codedrill.controlplane.identity

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp
import java.time.Clock
import java.util.UUID

/**
 * 비밀번호 재설정 (docs/ui-overhaul.md §6.9, §11.3).
 *
 * 1. **요청은 언제나 같은 답이다.** 계정이 있든 없든 "보냈다"고 답한다 — 다르게 답하면 그것만으로 어떤
 *    이메일이 가입돼 있는지 확인할 수 있다 (로그인이 실패 사유를 나누지 않는 것과 같다).
 * 2. **토큰은 해시만 둔다.** 평문은 링크에만 있다. 한 번 쓰면 끝, [IdentityProperties.resetTtl] 뒤면 끝,
 *    새로 요청하면 앞의 것도 끝.
 * 3. **재설정하면 열린 세션을 전부 끊는다.** 재설정의 태반은 "누가 내 계정을 쓰는 것 같다"이다.
 *
 * 보내는 일은 [PasswordResetDelivery] 의 몫이다. 메일 발송은 아직 없다 — 기본 구현은 아무것도 보내지
 * 않고, 개발 스택만 링크를 로그에 남긴다 ([IdentityProperties.logResetLinks]).
 */
@Service
class PasswordReset(
    private val jdbc: JdbcTemplate,
    private val repository: IdentityRepository,
    private val properties: IdentityProperties,
    private val delivery: PasswordResetDelivery,
    private val clock: Clock = Clock.systemUTC(),
) {
    private val passwords = BCryptPasswordEncoder()

    /** 있는 계정이면 링크를 보낸다. 없으면 아무것도 하지 않는다 — 부른 쪽은 그 차이를 모른다. */
    @Transactional
    fun request(email: String) {
        val (user, _) = repository.findCredentials(email.trim().lowercase()) ?: return
        val now = clock.instant()
        // 앞서 보낸 링크는 쓸 수 없게 — 메일함에 링크가 여럿이면 어느 것이 살아 있는지 사람이 모른다
        jdbc.update("UPDATE password_reset SET used_at = ? WHERE user_id = ? AND used_at IS NULL", Timestamp.from(now), user.id)
        val token = Tokens.issue()
        jdbc.update(
            "INSERT INTO password_reset (token_hash, user_id, expires_at) VALUES (?, ?, ?)",
            Tokens.hash(token), user.id, Timestamp.from(now.plus(properties.resetTtl)),
        )
        delivery.send(user.email, "${properties.resetLinkBase.trimEnd('/')}/reset-password?token=$token")
    }

    /** 새 비밀번호로 바꾼다. 토큰이 없거나 썼거나 지났으면 [Outcome.Invalid] — 셋을 가르지 않는다. */
    @Transactional
    fun reset(token: String, next: String): Outcome {
        if (next.length < IdentityService.MIN_PASSWORD_LENGTH) {
            return Outcome.Invalid("비밀번호는 ${IdentityService.MIN_PASSWORD_LENGTH}자 이상이어야 한다")
        }
        val now = clock.instant()
        // 쓰는 것과 확인을 한 문장으로 — 같은 링크를 두 번 눌러도 한 번만 바뀐다
        val userId = jdbc.query(
            """
            UPDATE password_reset SET used_at = ?
             WHERE token_hash = ? AND used_at IS NULL AND expires_at > ?
            RETURNING user_id
            """.trimIndent(),
            { rs, _ -> rs.getObject("user_id", UUID::class.java) },
            Timestamp.from(now), Tokens.hash(token), Timestamp.from(now),
        ).firstOrNull() ?: return Outcome.Invalid(INVALID)
        // 지운 계정은 바꾸지 않는다 (updatePassword 가 deleted_at 을 본다)
        if (repository.updatePassword(userId, checkNotNull(passwords.encode(next))) == 0) return Outcome.Invalid(INVALID)
        val revoked = repository.revokeAllFor(userId, "password-reset")
        log.info("비밀번호를 재설정했다: {} — 세션 {}개 끊음", userId, revoked)
        return Outcome.Done
    }

    sealed interface Outcome {
        data object Done : Outcome
        data class Invalid(val reason: String) : Outcome
    }

    private companion object {
        const val INVALID = "쓸 수 없는 링크다. 이미 썼거나 시간이 지났다 — 다시 요청한다"
        val log = LoggerFactory.getLogger(PasswordReset::class.java)
    }
}

/**
 * 재설정 링크를 사람에게 닿게 하는 길. 메일·문자 같은 발송 수단이 생기면 조립 지점이 이 빈을 바꿔 낀다.
 */
fun interface PasswordResetDelivery {
    fun send(email: String, link: String)
}

@Configuration
class PasswordResetConfig {

    /**
     * 기본은 보내지 않는다. 발송 수단이 없는데 보낸 척하면 사용자는 오지 않는 메일을 기다린다 — 그래서
     * 로그에 "보내지 못했다"를 남긴다(링크는 남기지 않는다). 개발 스택만 링크를 로그에 남겨 흐름을 끝까지
     * 시험할 수 있게 한다 — 운영 로그에 재설정 링크가 남으면 로그를 읽는 사람이 남의 계정을 연다.
     */
    @Bean
    @ConditionalOnMissingBean(PasswordResetDelivery::class)
    fun passwordResetDelivery(properties: IdentityProperties): PasswordResetDelivery {
        val log = LoggerFactory.getLogger(PasswordResetDelivery::class.java)
        return if (properties.logResetLinks) {
            PasswordResetDelivery { email, link -> log.warn("[개발용] 비밀번호 재설정 링크 — {}: {}", email, link) }
        } else {
            PasswordResetDelivery { _, _ -> log.warn("비밀번호 재설정을 요청받았지만 발송 수단이 설정되지 않아 보내지 못했다") }
        }
    }
}
