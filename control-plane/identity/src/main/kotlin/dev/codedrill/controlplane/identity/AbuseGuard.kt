package dev.codedrill.controlplane.identity

import jakarta.servlet.http.HttpServletRequest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import java.security.MessageDigest
import java.sql.Timestamp
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * 가입·로그인 남용 방어 (§10.2, A6 의 남은 절반).
 *
 * 두 가지를 센다. 같은 곳에서 온 **가입**의 수 — 계정을 여러 개 만들어 쿼터를 늘리는 것을
 * 막는다. 한 계정에 대한 **로그인 실패**의 수 — 비밀번호를 무한히 시험하는 것을 막는다.
 * 같은 곳에서 온 로그인 실패도 센다; 계정을 바꿔 가며 시험하는 것은 계정별로는 안 보인다.
 *
 * "어디서"는 IP 인데, IP 는 저장하지 않는다. 소금과 함께 해시한 값만 둔다 — 같은 곳인지만
 * 알면 되고 어디인지는 몰라도 된다. 프록시 뒤에 있으면 [IdentityProperties.trustedProxy] 를
 * 켜야 `X-Forwarded-For` 를 본다; 안 켜면 프록시의 주소가 모두의 주소가 되어 첫 다섯 명
 * 뒤로 아무도 가입하지 못한다. 켰는데 프록시가 없으면 아무나 헤더로 출처를 꾸민다 — 그래서
 * 기본은 끄고, 배포 문서가 켜라고 말한다.
 *
 * [IdentityProperties.exemptOrigins] 는 개발 스택의 것이다. 스모크가 한 곳에서 계정 열둘을
 * 만든다. 배포에서는 비운다.
 */
@Component
class AbuseGuard(
    private val jdbc: JdbcTemplate,
    private val properties: IdentityProperties,
    private val clock: Clock = Clock.systemUTC(),
) {

    /** 요청의 출처. 프록시를 믿을 때만 헤더를 본다. */
    fun originOf(request: HttpServletRequest): Origin {
        val address = if (properties.trustedProxy) {
            request.getHeader("X-Forwarded-For")?.split(',')?.firstOrNull()?.trim()?.ifEmpty { null } ?: request.remoteAddr
        } else {
            request.remoteAddr
        }
        return Origin(hash(address), exempt = address in properties.exemptOrigins)
    }

    /** 이 출처에서 지금 가입해도 되나. 안 되면 몇 초 뒤에 되는지. */
    fun signupAllowed(origin: Origin): Long? {
        if (origin.exempt) return null
        return retryAfter(Kind.REGISTER, origin.hash, null, properties.signupsPerHour, HOUR)
    }

    /** 이 계정으로, 이 출처에서 지금 로그인을 시험해도 되나. */
    fun loginAllowed(origin: Origin, subject: String): Long? {
        retryAfter(Kind.LOGIN_FAILED, null, subject, properties.loginFailuresPerAccount, QUARTER)?.let { return it }
        if (origin.exempt) return null
        return retryAfter(Kind.LOGIN_FAILED, origin.hash, null, properties.loginFailuresPerOrigin, QUARTER)
    }

    fun recordSignup(origin: Origin) = record(Kind.REGISTER, origin, null)

    fun recordLoginFailure(origin: Origin, subject: String) = record(Kind.LOGIN_FAILED, origin, subject)

    private fun record(kind: Kind, origin: Origin, subject: String?) {
        jdbc.update(
            "INSERT INTO auth_attempt (id, kind, origin_hash, subject, created_at) VALUES (?, ?, ?, ?, ?)",
            UUID.randomUUID(), kind.name, origin.hash, subject, Timestamp.from(clock.instant()),
        )
        // 하루 지난 줄은 쓸모가 없다. 기록할 때마다 조금씩 지운다 — 따로 청소 작업을 두지 않는다.
        jdbc.update("DELETE FROM auth_attempt WHERE created_at < ?", Timestamp.from(clock.instant().minus(DAY)))
    }

    /** 창 안의 수가 한도 이상이면, 가장 오래된 것이 창을 벗어날 때까지의 초. */
    private fun retryAfter(kind: Kind, originHash: String?, subject: String?, limit: Int, window: Duration): Long? {
        val since = clock.instant().minus(window)
        val stamps: List<Instant> = if (originHash != null) {
            jdbc.query(
                "SELECT created_at FROM auth_attempt WHERE kind = ? AND origin_hash = ? AND created_at >= ? ORDER BY created_at",
                { rs, _ -> rs.getTimestamp(1).toInstant() }, kind.name, originHash, Timestamp.from(since),
            )
        } else {
            jdbc.query(
                "SELECT created_at FROM auth_attempt WHERE kind = ? AND subject = ? AND created_at >= ? ORDER BY created_at",
                { rs, _ -> rs.getTimestamp(1).toInstant() }, kind.name, subject, Timestamp.from(since),
            )
        }
        if (stamps.size < limit) return null
        val oldestInWindow = stamps[stamps.size - limit]
        return maxOf(1L, Duration.between(clock.instant(), oldestInWindow.plus(window)).seconds)
    }

    private fun hash(address: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(properties.originSalt.toByteArray())
        digest.update(address.toByteArray())
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    data class Origin(val hash: String, val exempt: Boolean)

    private enum class Kind { REGISTER, LOGIN_FAILED }

    private companion object {
        val HOUR: Duration = Duration.ofHours(1)
        val QUARTER: Duration = Duration.ofMinutes(15)
        val DAY: Duration = Duration.ofDays(1)
    }
}
