package dev.codedrill.controlplane.profile

import org.springframework.stereotype.Service
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

/**
 * 공개 프로필을 읽는다.
 *
 * **비공개 프로필은 없는 것이다.** 남이 비공개 핸들을 열면 "없다"와 같은 답을 받는다 — "있지만 비공개"
 * 라고 답하면 그 핸들을 쓰는 사람이 있다는 사실이 샌다.
 */
@Service
class ProfileService(
    private val sources: ProfileSources,
    private val clock: Clock = Clock.systemUTC(),
    private val zone: ZoneId = ZoneId.of("Asia/Seoul"),
) {

    fun profile(readerId: String?, handle: String): PublicProfile? {
        val owner = sources.owner(handle) ?: return null
        val mine = readerId != null && readerId == owner.userId
        if (!owner.public && !mine) return null

        val today = LocalDate.now(clock.withZone(zone))
        val activity = fill(sources.activity(owner.userId, today.minusDays(DAYS - 1)), today)
        return PublicProfile(
            handle = owner.handle,
            displayName = owner.displayName,
            joinedAt = owner.joinedAt,
            mine = mine,
            public = owner.public,
            solved = sources.solved(owner.userId),
            activity = activity,
            streak = Streak(current = sources.currentStreak(owner.userId), longest = longest(activity)),
            rating = sources.rating(owner.userId),
            solutions = sources.solutions(owner.userId, SOLUTION_LIMIT),
            contributorTier = sources.contributorTier(owner.userId),
        )
    }

    companion object {
        /** 히트맵 한 장 — 1년. */
        const val DAYS = 365L
        const val SOLUTION_LIMIT = 20

        /** 빈 날을 0 으로 채운 1년. 오래된 날부터. */
        fun fill(counts: Map<LocalDate, Int>, today: LocalDate): List<Day> =
            (DAYS - 1 downTo 0).map { back -> today.minusDays(back).let { Day(it, counts[it] ?: 0) } }

        /**
         * 1년 안 가장 긴 연속. 처방의 스트릭과 같은 규칙이다 — 하루 빈 날은 건너뛰되 세지 않고, 이틀 연속
         * 비면 끊는다. 규칙이 다르면 훈련 화면과 프로필이 다른 숫자를 말한다.
         */
        fun longest(days: List<Day>): Int {
            var best = 0
            var run = 0
            var gap = 0
            for (day in days) {
                if (day.submissions > 0) {
                    run += 1
                    gap = 0
                } else {
                    gap += 1
                    if (gap >= 2) run = 0
                }
                best = maxOf(best, run)
            }
            return best
        }
    }
}
