package dev.codedrill.platform.messaging

/**
 * 브로커로 전달되는 작업 봉투 (기술 설계서 §15.3).
 *
 * 작업은 at-least-once 로 전달되므로 소비자는 [idempotencyKey] 기준으로 멱등해야 한다.
 * 소비자는 N/N-1 스키마를 함께 지원하고, 모르는 필드는 무시한다.
 */
data class JobEnvelope<T>(
    val schemaVersion: String,
    val jobType: String,
    val idempotencyKey: String,
    val correlationId: String,
    val payload: T,
)
