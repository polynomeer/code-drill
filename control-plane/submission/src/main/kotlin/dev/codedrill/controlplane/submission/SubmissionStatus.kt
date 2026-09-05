package dev.codedrill.controlplane.submission

/**
 * 제출 상태 머신 (기술 설계서 §4.2).
 *
 * ```
 * CREATED → QUEUED → LEASED → COMPILING → RUNNING → AGGREGATING → COMPLETED
 * ```
 *
 * 불변식:
 * - [COMPLETED] 는 종착이다. 재채점은 상태를 되돌리지 않고 새 revision 을 만든다.
 * - [SYSTEM_ERROR] 는 사용자 코드 실패와 다른 축이다. 플랫폼 장애를 오답으로 덮지 않는다 (§4.4).
 * - 전이는 현재 상태와 `version` 컬럼을 조건으로 한 낙관적 갱신으로만 적용한다 (§3.2).
 */
enum class SubmissionStatus {
    CREATED,
    QUEUED,
    LEASED,
    COMPILING,
    RUNNING,
    AGGREGATING,
    COMPLETED,
    CANCELLED,
    SYSTEM_ERROR,
    ;

    /** 더 이상 전이가 없는 상태. */
    val terminal: Boolean get() = allowedNext.isEmpty()

    /** 이 상태에서 전이할 수 있는 상태들. 표에 없는 전이는 전부 불법이다. */
    val allowedNext: Set<SubmissionStatus>
        get() = when (this) {
            CREATED -> setOf(QUEUED, CANCELLED)
            QUEUED -> setOf(LEASED, CANCELLED)
            // lease 만료 시 attempt 를 늘리며 QUEUED 로 되돌아간다 (§4.3).
            LEASED -> setOf(COMPILING, QUEUED, SYSTEM_ERROR)
            // 컴파일 실패는 테스트 실행 없이 종료한다 (§4.4).
            COMPILING -> setOf(RUNNING, COMPLETED, SYSTEM_ERROR)
            RUNNING -> setOf(AGGREGATING, COMPLETED, SYSTEM_ERROR)
            AGGREGATING -> setOf(COMPLETED, SYSTEM_ERROR)
            COMPLETED, CANCELLED, SYSTEM_ERROR -> emptySet()
        }

    fun canTransitionTo(next: SubmissionStatus): Boolean = next in allowedNext

    /** 불법 전이를 호출부가 조용히 넘기지 못하도록 예외로 막는다. */
    fun transitionTo(next: SubmissionStatus): SubmissionStatus {
        require(canTransitionTo(next)) { "불법 상태 전이: $this → $next" }
        return next
    }
}
