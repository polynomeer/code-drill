package dev.codedrill.platform.common

/**
 * 생성 요청의 멱등 키 (기술 설계서 §9.1).
 *
 * 제출은 `UNIQUE(user_id, idempotency_key)` 로 중복 제출 버튼을 흡수한다 (§4.3, §8.2).
 */
@JvmInline
value class IdempotencyKey(val value: String) {
    init {
        require(value.length in 1..128) { "idempotency key 길이는 1..128 이다: ${value.length}" }
    }
}
