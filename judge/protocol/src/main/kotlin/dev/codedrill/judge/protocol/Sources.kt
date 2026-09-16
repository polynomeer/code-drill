package dev.codedrill.judge.protocol

import java.security.MessageDigest

/**
 * 오브젝트 스토어의 소스 하나 (기술 설계서 §8.3).
 *
 * 제출 소스는 메시지에 실리지 않는다 — 번들과 같은 이유다. 브로커의 메시지 한계에 걸리기
 * 전에, 소스는 사용자의 것이고 브로커의 큐·데드레터·로그에 복제되면 안 된다 (§11.1, §11.3).
 * 메시지에는 키와 digest 만 실리고, Runner 가 받아 digest 를 대조한다.
 */
data class SourceRef(val key: String, val digest: String)

object Sources {

    const val CONTENT_TYPE = "text/plain; charset=utf-8"

    /** 제출 id 가 키다. 재채점은 같은 소스를 다시 채점하므로 같은 키를 다시 쓴다. */
    fun key(submissionId: String) = "sources/$submissionId.txt"

    fun bytes(source: String): ByteArray = source.toByteArray(Charsets.UTF_8)

    fun digest(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    fun ref(submissionId: String, source: String): SourceRef = SourceRef(key(submissionId), digest(bytes(source)))
}
