package dev.codedrill.platform.common

/**
 * 외부 API 오류 코드 (기술 설계서 §9.4).
 *
 * 클라이언트가 재시도 여부를 코드만 보고 판단할 수 있도록 [retry] 를 함께 노출한다.
 * 플랫폼 장애를 사용자 코드 실패로 덮지 않는 것이 이 분류의 목적이다 (§4.4).
 */
enum class ErrorCode(val category: Category, val retry: Retry) {
    INVALID_SIGNATURE(Category.VALIDATION, Retry.AFTER_FIX),
    SOURCE_TOO_LARGE(Category.VALIDATION, Retry.AFTER_FIX),

    LANGUAGE_NOT_ALLOWED(Category.POLICY, Retry.AFTER_POLICY_WINDOW),
    QUOTA_EXCEEDED(Category.POLICY, Retry.AFTER_POLICY_WINDOW),

    DRAFT_VERSION_CONFLICT(Category.CONFLICT, Retry.AFTER_REFRESH),
    PROBLEM_VERSION_STALE(Category.CONFLICT, Retry.AFTER_REFRESH),

    COMPILE_ERROR(Category.JUDGE, Retry.AFTER_FIX),
    TIME_LIMIT(Category.JUDGE, Retry.AFTER_FIX),

    JUDGE_UNAVAILABLE(Category.PLATFORM, Retry.AUTOMATIC),
    ARTIFACT_FETCH_FAILED(Category.PLATFORM, Retry.AUTOMATIC),
    ;

    enum class Category { VALIDATION, POLICY, CONFLICT, JUDGE, PLATFORM }

    /** 재시도 조건. 사용자 수정이 필요한 실패와 플랫폼이 스스로 회복할 실패를 가른다. */
    enum class Retry { AFTER_FIX, AFTER_POLICY_WINDOW, AFTER_REFRESH, AUTOMATIC }
}

/** 표준 오류 응답 본문 (§9.1). traceId 로 로그·트레이스와 연결한다. */
data class ApiError(
    val errorCode: ErrorCode,
    val message: String,
    val traceId: String,
)
