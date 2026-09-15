package dev.codedrill.controlplane.identity

import dev.codedrill.platform.common.Principal
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.UUID

/**
 * 인증 (기술 설계서 §11.2, §11.3).
 *
 * 세 가지를 지킨다.
 *
 * 1. **비밀번호는 되돌릴 수 없게 저장한다.** BCrypt 해시만 남기며, 평문은 요청을
 *    처리하는 동안에만 존재한다.
 * 2. **토큰은 저장소에도 평문으로 두지 않는다.** SHA-256 해시로 조회하므로, DB 를
 *    읽을 수 있게 된 공격자가 그대로 로그인하지 못한다.
 * 3. **refresh 는 회전한다.** 한 번 쓰면 그 자리에서 무효가 되고 새 것이 나온다.
 *    이미 쓴 refresh 가 다시 오면 토큰이 복제됐다는 뜻이므로 세션을 통째로 끊는다.
 */
@Service
class IdentityService(
    private val repository: IdentityRepository,
    private val properties: IdentityProperties,
    private val areas: List<PersonalData> = emptyList(),
    private val clock: Clock = Clock.systemUTC(),
) {

    private val log = LoggerFactory.getLogger(javaClass)
    private val passwords = BCryptPasswordEncoder()
    private val random = SecureRandom()

    @Transactional
    fun register(email: String, displayName: String, password: String): Registration {
        val normalized = normalize(email)
        require(password.length >= MIN_PASSWORD_LENGTH) {
            "비밀번호는 ${MIN_PASSWORD_LENGTH}자 이상이어야 한다"
        }

        val user = repository.insertUser(
            id = UUID.randomUUID(),
            email = normalized,
            displayName = displayName.ifBlank { normalized.substringBefore('@') },
            passwordHash = passwords.encode(password),
        ) ?: return Registration.EmailTaken

        return Registration.Created(issue(user))
    }

    /**
     * 표시 이름을 바꾼다 (기획서 부록 A 계정 도메인).
     *
     * 이메일은 바꾸지 않는다. 이메일은 로그인 식별자이자 유일 제약이라, 바꾸는 것은
     * 소유 확인(새 주소로 보낸 링크)이 함께 있어야 하는 별개의 흐름이다. 그것 없이
     * 바꾸게 하면 남의 주소를 자기 계정에 붙일 수 있다.
     */
    @Transactional
    fun rename(userId: String, displayName: String): Boolean {
        require(displayName.isNotBlank()) { "표시 이름은 비울 수 없다" }
        return repository.updateDisplayName(UUID.fromString(userId), displayName.trim()) > 0
    }

    /**
     * 비밀번호를 바꾼다.
     *
     * 지금 비밀번호를 다시 받는다. 자리를 비운 사이 남이 만졌을 때, 막을 것이 열려 있는
     * 세션 하나뿐이면 안 된다 — 계정 삭제와 같은 이유다.
     *
     * **바꾸면 열린 세션을 전부 끊고 새로 하나 내준다.** 비밀번호를 바꾸는 이유의 태반이
     * "누가 내 계정을 보고 있는 것 같다"이고, 그때 바꾸기만 하고 열린 세션을 그대로 두면
     * 바꾼 의미가 없다.
     *
     * 지금 쓰던 세션도 함께 끊는다. 남겨 두려면 "이 세션만 빼고"를 어딘가에서 판단해야
     * 하는데, 그 판단이 틀리면 **끊었어야 할 세션이 살아남는다.** 대신 새 세션을 돌려주어
     * 사용자가 다시 로그인하지 않게 한다.
     */
    @Transactional
    fun changePassword(userId: String, current: String, next: String): PasswordChange {
        require(next.length >= MIN_PASSWORD_LENGTH) {
            "비밀번호는 ${MIN_PASSWORD_LENGTH}자 이상이어야 한다"
        }
        val id = UUID.fromString(userId)
        val user = repository.findById(id) ?: return PasswordChange.NotFound
        val credentials = repository.findCredentials(normalize(user.email))
            ?: return PasswordChange.NotFound
        if (!passwords.matches(current, credentials.second)) return PasswordChange.WrongPassword

        repository.updatePassword(id, passwords.encode(next))
        val revoked = repository.revokeAllFor(id, "password-changed")
        return PasswordChange.Done(issue(user), revoked)
    }

    sealed interface PasswordChange {
        /** [revoked] 는 끊긴 세션 수. 사용자에게 "다른 기기에서 로그아웃됐다"를 말해 준다. */
        data class Done(val session: IssuedSession, val revoked: Int) : PasswordChange
        data object WrongPassword : PasswordChange
        data object NotFound : PasswordChange
    }

    /**
     * 계정을 지운다 (§11.3).
     *
     * **비밀번호를 다시 받는다.** 되돌릴 수 없는 요청이고, 자리를 비운 사이 남이 만졌을
     * 때 막을 것이 세션 하나뿐이면 안 된다.
     *
     * 행을 지우지 않고 식별값을 지운다. 제출과 판정 이력이 이 id 를 참조하므로(§8.1),
     * 행을 지우면 문제별 통계까지 함께 사라진다 — 그건 지워 달라고 요청받은 것이 아니다.
     * 각 모듈이 자기 몫의 개인 데이터를 지우는 것은 [areas] 가 맡는다.
     */
    @Transactional
    fun deleteAccount(userId: String, password: String): Deletion {
        val id = UUID.fromString(userId)
        val user = repository.findById(id) ?: return Deletion.NotFound
        val credentials = repository.findCredentials(normalize(user.email))
            ?: return Deletion.NotFound
        if (!passwords.matches(password, credentials.second)) return Deletion.WrongPassword

        val erased = buildMap {
            for (area in areas) {
                putAll(area.erase(userId).mapKeys { (key, _) -> "${area.area}.$key" })
            }
            put("sessions", repository.revokeAllFor(id, "account-deleted"))
            put("account", repository.anonymize(id, tombstone(id)))
        }
        log.info("계정을 지웠다: {} — {}", userId, erased)
        return Deletion.Done(erased)
    }

    /**
     * 지운 계정이 남기는 이메일 자리값.
     *
     * 비우지 않는 이유는 이메일이 유니크이기 때문이다 — 비워 두면 두 번째 삭제가 충돌하고,
     * 원래 주소를 남기면 지운 것이 아니다. 실제 주소와 겹칠 수 없는 도메인을 쓴다.
     */
    private fun tombstone(id: UUID) = "deleted-$id@deleted.invalid"

    sealed interface Deletion {
        data class Done(val erased: Map<String, Int>) : Deletion
        data object WrongPassword : Deletion
        data object NotFound : Deletion
    }

    /** 이 사람에 대해 갖고 있는 것 전부 (§11.3 반출). */
    fun exportAccount(userId: String): Map<String, Any?>? {
        val user = repository.findById(UUID.fromString(userId)) ?: return null
        return buildMap {
            put("account", mapOf("id" to user.id.toString(), "email" to user.email,
                                 "displayName" to user.displayName))
            for (area in areas) put(area.area, area.export(userId))
        }
    }

    /**
     * 이메일로 계정 id 를 찾는다.
     *
     * 관리자 역할의 **부트스트랩에만** 쓴다 (§11.2). 일반 조회로 열어 두면 이메일 존재
     * 여부를 확인하는 창구가 되므로(§11.1 계정 열거), 부르는 쪽은 결과를 응답으로
     * 흘리지 않아야 한다.
     */
    fun findIdByEmail(email: String): String? =
        repository.findCredentials(normalize(email))?.first?.id?.toString()

    /**
     * 로그인.
     *
     * 이메일이 없을 때도 해시 검증을 한 번 돌린다. 없는 계정만 빨리 실패하면 응답
     * 시간만으로 가입 여부를 알아낼 수 있다 (§11.1 계정 열거).
     */
    @Transactional
    fun login(email: String, password: String): IssuedSession? {
        val found = repository.findCredentials(normalize(email))
        if (found == null) {
            passwords.matches(password, DUMMY_HASH)
            return null
        }

        val (user, hash) = found
        if (!passwords.matches(password, hash)) return null
        return issue(user)
    }

    /**
     * access token 으로 호출자를 찾는다. 요청마다 불린다.
     *
     * 만료와 폐기를 구분해 돌려준다. 클라이언트는 만료면 refresh 로 갱신하고 같은
     * 요청을 다시 보내면 되지만, 폐기면 다시 로그인해야 한다 (§9.4).
     */
    fun resolve(accessToken: String): Resolution {
        val session = repository.findByAccessHash(hash(accessToken)) ?: return Resolution.Unknown
        if (session.revokedAt != null) return Resolution.Revoked
        if (clock.instant().isAfter(session.accessExpiresAt)) return Resolution.Expired

        val user = repository.findById(session.userId) ?: return Resolution.Unknown
        return Resolution.Active(Principal(user.id.toString(), user.displayName))
    }

    /**
     * 갱신 (§11.2 회전 가능한 refresh token).
     *
     * 쓰는 순간 이전 세션을 끊고 새 세션을 낸다. 이미 끊긴 refresh 가 다시 오면
     * 정상적인 클라이언트에서는 일어날 수 없는 일이므로, 그 사용자의 세션을 전부
     * 끊는다 — 토큰이 복제됐다는 가장 흔한 신호다.
     */
    @Transactional
    fun refresh(refreshToken: String): IssuedSession? {
        val session = repository.findByRefreshHash(hash(refreshToken)) ?: return null

        if (session.revokedAt != null) {
            log.warn("이미 쓴 refresh token 이 다시 왔다. 세션을 전부 끊는다: user={}", session.userId)
            repository.revokeAllFor(session.userId, "refresh 재사용 탐지")
            return null
        }
        if (clock.instant().isAfter(session.refreshExpiresAt)) return null

        repository.revoke(session.id, "회전")
        val user = repository.findById(session.userId) ?: return null
        return issue(user)
    }

    @Transactional
    fun logout(accessToken: String): Boolean {
        val session = repository.findByAccessHash(hash(accessToken)) ?: return false
        return repository.revoke(session.id, "로그아웃") > 0
    }

    private fun issue(user: User): IssuedSession {
        val access = token()
        val refresh = token()
        val now = clock.instant()
        val accessExpiry = now.plus(properties.accessTtl)
        val refreshExpiry = now.plus(properties.refreshTtl)

        repository.insertSession(
            id = UUID.randomUUID(),
            userId = user.id,
            accessHash = hash(access),
            refreshHash = hash(refresh),
            accessExpiresAt = accessExpiry,
            refreshExpiresAt = refreshExpiry,
        )
        return IssuedSession(access, refresh, accessExpiry, refreshExpiry, user)
    }

    /** 256비트 난수. 추측할 수 없어야 하므로 UUID 가 아니라 [SecureRandom] 이다. */
    private fun token(): String = ByteArray(TOKEN_BYTES)
        .also(random::nextBytes)
        .let { Base64.getUrlEncoder().withoutPadding().encodeToString(it) }

    private fun hash(token: String): String = MessageDigest.getInstance("SHA-256")
        .digest(token.toByteArray())
        .joinToString("") { "%02x".format(it) }

    /** 대소문자와 앞뒤 공백만 정규화한다. 그 이상 손대면 남의 주소가 될 수 있다. */
    private fun normalize(email: String): String = email.trim().lowercase()

    /** 가입 한도에 닿았다. 몇 초 뒤에 다시 올 수 있는지 함께 (§10.2). */
    sealed interface Registration {
        data class Created(val session: IssuedSession) : Registration
        data object EmailTaken : Registration
    }

    sealed interface Resolution {
        data class Active(val user: Principal) : Resolution
        data object Expired : Resolution
        data object Revoked : Resolution
        data object Unknown : Resolution
    }

    private companion object {
        const val TOKEN_BYTES = 32
        const val MIN_PASSWORD_LENGTH = 10

        /**
         * 없는 계정에도 검증 비용을 들이기 위한 더미 해시.
         *
         * 어떤 비밀번호와도 맞지 않는다. 값 자체는 의미가 없고, BCrypt 를 한 번 돌리는
         * 것이 목적이다.
         */
        val DUMMY_HASH = BCryptPasswordEncoder().encode("계정 열거를 막기 위한 더미")
    }
}

/**
 * 토큰 수명 (§11.2 "짧은 access token").
 *
 * access 를 짧게 두는 이유는 폐기가 아니라 **노출 시간**이다. 불투명 토큰이라 폐기는
 * 즉시 되지만, 유출을 알아차리지 못한 구간은 짧을수록 좋다.
 */
@ConfigurationProperties(prefix = "codedrill.auth")
data class IdentityProperties(
    val accessTtl: Duration = Duration.ofMinutes(30),
    val refreshTtl: Duration = Duration.ofDays(14),
    // --- 남용 방어 (§10.2, A6). 이유와 함정은 [AbuseGuard] 에. ---
    /** 같은 출처에서 한 시간에 몇 번 가입할 수 있나. 사람은 한 번이다. */
    val signupsPerHour: Int = 5,
    /** 한 계정에 15분 안에 몇 번 틀릴 수 있나. */
    val loginFailuresPerAccount: Int = 10,
    /** 같은 출처에서 15분 안에 몇 번 틀릴 수 있나 — 계정을 바꿔 가며 시험하는 것. */
    val loginFailuresPerOrigin: Int = 30,
    /** 프록시 뒤인가. 켜면 X-Forwarded-For 의 첫 주소를 출처로 본다. */
    val trustedProxy: Boolean = false,
    /** 출처 해시의 소금. 배포마다 다르게 준다 — 같으면 해시로 IP 를 되짚을 수 있다. */
    val originSalt: String = "dev",
    /** 세지 않는 출처. 개발 스택의 것이고 배포에서는 비운다. */
    val exemptOrigins: List<String> = emptyList(),
)
