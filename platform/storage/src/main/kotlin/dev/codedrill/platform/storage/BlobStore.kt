package dev.codedrill.platform.storage

/**
 * 오브젝트 스토어 (기술 설계서 §8.3).
 *
 * 소스·테스트 번들·트레이스처럼 **크고 불변인 것**이 여기 간다. DB 와 메시지에는 참조와
 * digest 만 남는다 — 그래야 제출이 늘어도 DB 가 그만큼 커지지 않고, 큰 문제의 테스트가
 * 브로커의 메시지 크기 한계에 먼저 걸리지 않는다.
 *
 * 키는 내용으로 정한다 (`bundles/<digest>`). 같은 내용은 같은 키이므로 두 번 올려도 해가
 * 없고, 읽는 쪽은 digest 로 받은 것이 맞는지 스스로 확인한다.
 */
interface BlobStore {

    /** [digest] 는 내용의 SHA-256 이다. 함께 저장해 두면 내용을 다시 받지 않고도 맞는지 물을 수 있다. */
    fun put(key: String, bytes: ByteArray, contentType: String = "application/octet-stream", digest: String? = null)

    /** 없으면 null. 없는 것과 못 읽는 것은 다르다 — 못 읽으면 예외다. */
    fun get(key: String): ByteArray?

    /**
     * 저장돼 있는 것의 digest. 없으면 null.
     *
     * 있는지만 묻지 않는 이유가 있다. 스토어에서 손상되거나 손댄 번들은 "있다"고 답하고,
     * 그러면 올리는 쪽은 멀쩡하다고 믿고 받는 쪽은 매번 시스템 오류로 끝난다 — 아무도
     * 고치지 않는다.
     */
    fun digestOf(key: String): String?

    fun exists(key: String): Boolean = digestOf(key) != null

    /** 지운다. 없어도 조용하다 — 삭제 요청(§11.3)은 두 번 와도 같은 결과여야 한다. */
    fun delete(key: String)
}
