package dev.codedrill.controlplane.identity

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/**
 * 비밀 토큰 (세션·비밀번호 재설정).
 *
 * 평문은 내주는 순간에만 있고, 저장소에는 SHA-256 해시만 남는다 — DB 를 읽게 된 공격자가 그대로 쓰지
 * 못하게. 세션과 재설정이 같은 규칙을 따르도록 한 곳에 둔다.
 */
internal object Tokens {
    private const val TOKEN_BYTES = 32
    private val random = SecureRandom()

    /** 256비트 난수. 추측할 수 없어야 하므로 UUID 가 아니라 [SecureRandom] 이다. */
    fun issue(): String = ByteArray(TOKEN_BYTES)
        .also(random::nextBytes)
        .let { Base64.getUrlEncoder().withoutPadding().encodeToString(it) }

    fun hash(token: String): String = MessageDigest.getInstance("SHA-256")
        .digest(token.toByteArray())
        .joinToString("") { "%02x".format(it) }
}
