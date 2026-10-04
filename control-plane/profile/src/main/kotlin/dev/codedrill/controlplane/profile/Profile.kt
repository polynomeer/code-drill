package dev.codedrill.controlplane.profile

import java.time.Instant
import java.time.LocalDate

/**
 * 공개 프로필 (docs/ui-overhaul.md §6.7).
 *
 * 상용 플랫폼이 다 보이는 것 — 푼 문제 수(난이도별), 1년 활동, 연속 일수, 레이팅, 공개한 풀이, 기여 등급.
 *
 * **역량 수준은 없다.** 본인에게만 보인다 — 공개 프로필에 내걸면 No false precision 과 어긋나는 비교가
 * 시작된다. 기여도 수치 대신 등급뿐이다 (§8.5 "남에게는 등급뿐").
 */
data class PublicProfile(
    val handle: String,
    val displayName: String,
    val joinedAt: Instant,
    /** 이 프로필의 주인이 읽고 있는가. 화면이 "공개 설정"과 "내 역량 지도" 같은 본인 전용 행동을 단다. */
    val mine: Boolean,
    /** 남에게 보이는가. 본인은 비공개여도 자기 프로필을 미리 본다. */
    val public: Boolean,
    val solved: Solved,
    /** 최근 1년, 하루 한 칸. 오래된 날부터. 활동이 없는 날도 0 으로 있다 — 비어 있는 것도 기록이다. */
    val activity: List<Day>,
    val streak: Streak,
    val rating: Rating?,
    val solutions: List<SharedSolution>,
    /** 기여 등급 (NEW·ACTIVE·TRUSTED). 수치는 보이지 않는다. */
    val contributorTier: String,
)

/** 푼 문제. 난이도 키는 문제 카탈로그의 것(INTRO…EXPERT) 그대로다. */
data class Solved(val total: Int, val byDifficulty: Map<String, Int>)

data class Day(val date: LocalDate, val submissions: Int)

/** 연속 일수 — 지금 이어지는 것과 1년 안 가장 긴 것. 규칙은 처방의 스트릭과 같다 (하루 빈 날은 넘긴다). */
data class Streak(val current: Int, val longest: Int)

/** 레이팅. 대회를 한 번도 치르지 않았으면 프로필에 없다 — 1500 은 실력이 아니라 출발점이다. */
data class Rating(val rating: Int, val contests: Int, val history: List<RatingPoint>)

data class RatingPoint(val contestId: String, val title: String, val rank: Int, val before: Int, val after: Int, val at: Instant)

/** 공개한 풀이의 목록. 본문은 문제 화면에서 — 맞히기 전에는 잠긴다는 규칙이 거기 있다. */
data class SharedSolution(val postId: String, val problemId: String, val title: String?, val helpful: Int, val at: Instant)

/** 프로필 주인의 신원. */
data class ProfileOwner(
    val userId: String,
    val handle: String,
    val displayName: String,
    val public: Boolean,
    val joinedAt: Instant,
)

/**
 * 공개 프로필이 기대는 사실들 (§3.1 조립 지점).
 *
 * 신원은 Identity, 푼 문제와 활동은 Submission·Problem, 연속 일수는 Learning, 레이팅은 Contest, 풀이와
 * 기여는 Workspace 의 것이다. Profile 은 그 어느 것도 직접 읽지 않는다.
 */
interface ProfileSources {
    fun owner(handle: String): ProfileOwner?
    fun solved(userId: String): Solved

    /** 하루별 제출 수. 활동이 없는 날은 빠져도 된다 — 채우는 것은 이 모듈이다. */
    fun activity(userId: String, since: LocalDate): Map<LocalDate, Int>
    fun currentStreak(userId: String): Int
    fun rating(userId: String): Rating?
    fun solutions(userId: String, limit: Int): List<SharedSolution>
    fun contributorTier(userId: String): String
}
