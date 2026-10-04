package dev.codedrill.controlplane.assembly

import dev.codedrill.controlplane.contest.ContestService
import dev.codedrill.controlplane.identity.IdentityService
import dev.codedrill.controlplane.learning.LearningService
import dev.codedrill.controlplane.problem.ProblemProgress
import dev.codedrill.controlplane.profile.ProfileOwner
import dev.codedrill.controlplane.profile.ProfileSources
import dev.codedrill.controlplane.profile.Rating
import dev.codedrill.controlplane.profile.RatingPoint
import dev.codedrill.controlplane.profile.SharedSolution
import dev.codedrill.controlplane.profile.Solved
import dev.codedrill.controlplane.workspace.DiscussionService
import dev.codedrill.platform.problempackage.ProblemPackageLoader
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDate

/**
 * 공개 프로필이 기대는 사실들 (§3.1 조립 지점, docs/ui-overhaul.md §6.7).
 *
 * 다섯 도메인의 사실을 Profile 모듈의 말로 옮긴다. ControlPlaneConfig 에 넣지 않고 따로 둔 이유는 그
 * 파일이 이미 한 번에 읽히지 않을 만큼 길어서다 — 새 조립은 여기처럼 도메인 하나에 파일 하나로 둔다.
 */
@Configuration
class ProfileAssembly {

    @Bean
    fun profileSources(
        jdbc: JdbcTemplate,
        identity: IdentityService,
        progress: ProblemProgress,
        packages: ProblemPackageLoader,
        learning: LearningService,
        contests: ContestService,
        discussions: DiscussionService,
    ) = object : ProfileSources {

        override fun owner(handle: String): ProfileOwner? = identity.findByHandle(handle)?.let { user ->
            ProfileOwner(
                userId = user.id.toString(),
                handle = user.handle ?: return@let null,
                displayName = user.displayName,
                public = user.profilePublic,
                joinedAt = user.createdAt,
            )
        }

        /** 난이도는 문제 카탈로그에서. 카탈로그를 못 읽는 문제(프로젝트형, 내려간 문제)는 세지 않는다. */
        override fun solved(userId: String): Solved {
            val difficulties = progress.solvedBy(userId).mapNotNull { id ->
                runCatching { packages.load(id).catalog.difficulty.name }.getOrNull()
            }
            return Solved(total = difficulties.size, byDifficulty = difficulties.groupingBy { it }.eachCount())
        }

        override fun activity(userId: String, since: LocalDate): Map<LocalDate, Int> = jdbc.query(
            """
            SELECT (created_at AT TIME ZONE 'Asia/Seoul')::date AS day, count(*) AS n
              FROM submission
             WHERE user_id = ? AND (created_at AT TIME ZONE 'Asia/Seoul')::date >= ?
             GROUP BY day
            """.trimIndent(),
            { rs, _ -> rs.getDate("day").toLocalDate() to rs.getInt("n") },
            userId, java.sql.Date.valueOf(since),
        ).toMap()

        override fun currentStreak(userId: String) = learning.streak(userId)

        /** 대회를 한 번도 치르지 않았으면 레이팅이 없다 — 출발점(1500)을 실력처럼 내걸지 않는다. */
        override fun rating(userId: String): Rating? = contests.rating(userId).takeIf { it.contests > 0 }?.let { rating ->
            Rating(
                rating = rating.rating,
                contests = rating.contests,
                history = rating.history.map {
                    RatingPoint(it.contestId.toString(), it.title, it.rank, it.before, it.after, it.appliedAt)
                },
            )
        }

        override fun solutions(userId: String, limit: Int) = discussions.solutionsBy(userId, limit).map {
            SharedSolution(it.id.toString(), it.problemId, it.title, it.helpful, it.createdAt)
        }

        override fun contributorTier(userId: String) = discussions.tierOf(userId).name
    }
}
