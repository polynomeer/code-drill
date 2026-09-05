package dev.codedrill.platform.observability

/**
 * 상관관계 식별자 체인 (기술 설계서 §13.1).
 *
 * ```
 * trace_id → request_id → submission_id → execution_id/attempt → sandbox_id → visual trace_id
 * ```
 *
 * 모든 로그와 이벤트는 이 키들을 구조화 필드로 실어 보낸다. 장애 조사 시 사용자 제출
 * 하나를 요청부터 샌드박스까지 한 줄로 잇는 것이 목적이다.
 */
object CorrelationIds {
    const val TRACE_ID = "trace_id"
    const val REQUEST_ID = "request_id"
    const val SUBMISSION_ID = "submission_id"
    const val EXECUTION_ID = "execution_id"
    const val ATTEMPT = "attempt"
    const val SANDBOX_ID = "sandbox_id"
    const val VISUAL_TRACE_ID = "visual_trace_id"

    /** 로그에 실어도 되는 키 전체. 소스 전문·토큰·숨은 입력은 여기에 없다 (§11.3). */
    val all: Set<String> = setOf(
        TRACE_ID, REQUEST_ID, SUBMISSION_ID, EXECUTION_ID, ATTEMPT, SANDBOX_ID, VISUAL_TRACE_ID,
    )
}
