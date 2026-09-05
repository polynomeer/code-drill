package dev.codedrill.judge.protocol

/**
 * 테스트·제출 판정 (기술 설계서 §14.1 골든 판정 행렬).
 *
 * [SYSTEM_ERROR] 는 사용자 코드 실패가 아니라 플랫폼 장애다. 집계에서 사용자 실패로
 * 덮이지 않도록 별도 축으로 다룬다 (§4.4).
 */
enum class Verdict(val userFailure: Boolean) {
    ACCEPTED(false),
    WRONG_ANSWER(true),
    COMPILE_ERROR(true),
    RUNTIME_ERROR(true),
    TIME_LIMIT(true),
    MEMORY_LIMIT(true),
    OUTPUT_LIMIT(true),
    SYSTEM_ERROR(false),
}
