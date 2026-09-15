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

    UNAUTHENTICATED(Category.AUTH, Retry.AFTER_FIX),
    /** 만료된 access token. 클라이언트는 refresh 로 갱신하고 같은 요청을 다시 보낸다. */
    TOKEN_EXPIRED(Category.AUTH, Retry.AFTER_REFRESH),
    /** 인증은 됐지만 그 자원의 주인이 아니다. */
    FORBIDDEN(Category.AUTH, Retry.AFTER_FIX),

    LANGUAGE_NOT_ALLOWED(Category.POLICY, Retry.AFTER_POLICY_WINDOW),
    QUOTA_EXCEEDED(Category.POLICY, Retry.AFTER_POLICY_WINDOW),
    /** 제재 중인 계정의 쓰기 요청 (§8.5 단계적 제재). 기간이 끝나거나 이의가 받아들여지면 풀린다. */
    ACCOUNT_SANCTIONED(Category.POLICY, Retry.AFTER_POLICY_WINDOW),

    DRAFT_VERSION_CONFLICT(Category.CONFLICT, Retry.AFTER_REFRESH),
    PROBLEM_VERSION_STALE(Category.CONFLICT, Retry.AFTER_REFRESH),

    /**
     * 요청은 멀쩡한데 그 기능에 필요한 콘텐츠가 이 문제에 없다.
     *
     * VALIDATION 이 아니다. 사용자가 입력을 고쳐도 달라지지 않고, 고칠 사람은 저작자다 —
     * 그래서 [Retry.AFTER_FIX] 의 "fix" 는 여기서만 사용자의 것이 아니다.
     */
    CONTENT_UNAVAILABLE(Category.CONFLICT, Retry.AFTER_FIX),

    COMPILE_ERROR(Category.JUDGE, Retry.AFTER_FIX),
    TIME_LIMIT(Category.JUDGE, Retry.AFTER_FIX),

    JUDGE_UNAVAILABLE(Category.PLATFORM, Retry.AUTOMATIC),
    ARTIFACT_FETCH_FAILED(Category.PLATFORM, Retry.AUTOMATIC),
    ;

    enum class Category { VALIDATION, AUTH, POLICY, CONFLICT, JUDGE, PLATFORM }

    /** 재시도 조건. 사용자 수정이 필요한 실패와 플랫폼이 스스로 회복할 실패를 가른다. */
    enum class Retry { AFTER_FIX, AFTER_POLICY_WINDOW, AFTER_REFRESH, AUTOMATIC }
}

/** 표준 오류 응답 본문 (§9.1). traceId 로 로그·트레이스와 연결한다. */
data class ApiError(
    val errorCode: ErrorCode,
    val message: String,
    val traceId: String,
)
