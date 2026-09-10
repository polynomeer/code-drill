package dev.codedrill.controlplane.coaching

/**
 * 이 사용자가 이미 통과한 문제 (기술 설계서 §3.1 조립 지점).
 *
 * Coaching 은 제출을 모른다. 변형 문제를 고를 때 "이미 푼 것은 빼야 한다"만 알면 되고,
 * 그 사실을 아는 곳은 제출 도메인이다.
 */
fun interface SolvedProblems {
    fun solvedBy(userId: String): Set<String>

    companion object {
        val NONE = SolvedProblems { emptySet() }
    }
}

/**
 * 변형 문제로 낼 수 있는 문제 (§3.1 조립 지점).
 *
 * 공개된 것만 낸다. 콘텐츠 디렉터리를 직접 훑으면 아직 공개되지 않은 문제가 과제로
 * 나가고, 사용자는 열 수 없는 문제를 풀라는 말을 듣는다.
 */
fun interface CoachableProblems {
    fun ids(): Set<String>

    companion object {
        val NONE = CoachableProblems { emptySet() }
    }
}
