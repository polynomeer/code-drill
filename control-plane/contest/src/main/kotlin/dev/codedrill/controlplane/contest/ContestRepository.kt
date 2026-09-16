package dev.codedrill.controlplane.contest

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

@Repository
class ContestRepository(private val jdbc: JdbcTemplate) {

    fun insert(c: Contest, problemIds: List<String>) {
        jdbc.update(
            """
            INSERT INTO contest (id, kind, title, created_by, starts_at, ends_at, minutes, published, join_code, parent_id, rated)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            c.id, c.kind.name, c.title, c.createdBy, c.startsAt?.let { Timestamp.from(it) }, c.endsAt?.let { Timestamp.from(it) },
            c.minutes, c.published, c.joinCode, c.parentId, c.rated,
        )
        problemIds.forEachIndexed { i, pid ->
            jdbc.update("INSERT INTO contest_problem (contest_id, problem_id, ord) VALUES (?, ?, ?)", c.id, pid, i)
        }
    }

    fun find(id: UUID): Contest? = jdbc.query("SELECT * FROM contest WHERE id = ?", CONTEST, id).firstOrNull()

    fun findByCode(code: String): Contest? = jdbc.query("SELECT * FROM contest WHERE join_code = ?", CONTEST, code).firstOrNull()

    /** 공개된 대회들, 최근 시작 순. 대결은 내가 참가한 것만. */
    fun visible(userId: String, limit: Int): List<Contest> = jdbc.query(
        """
        SELECT c.* FROM contest c
         WHERE (c.kind IN ('CONTEST', 'HACK') AND c.published)
            OR (c.kind IN ('DUEL', 'VIRTUAL') AND EXISTS (SELECT 1 FROM contest_entry e WHERE e.contest_id = c.id AND e.user_id = ?))
         ORDER BY c.starts_at DESC NULLS FIRST, c.created_at DESC LIMIT ?
        """.trimIndent(),
        CONTEST, userId, limit,
    )

    /** 이 사람이 이 대회로 연 가상 참가 중 아직 도는 것. 하나면 된다. */
    fun runningVirtual(parentId: UUID, userId: String, now: Instant): Contest? = jdbc.query(
        "SELECT * FROM contest WHERE parent_id = ? AND created_by = ? AND ends_at > ? ORDER BY created_at DESC LIMIT 1",
        CONTEST, parentId, userId, Timestamp.from(now),
    ).firstOrNull()

    fun problems(contestId: UUID): List<String> =
        jdbc.query("SELECT problem_id FROM contest_problem WHERE contest_id = ? ORDER BY ord", { rs, _ -> rs.getString(1) }, contestId)

    fun publish(id: UUID): Int = jdbc.update("UPDATE contest SET published = TRUE WHERE id = ? AND NOT published", id)

    /** 대결의 시작. 둘째가 붙는 순간이다. 이미 시작했으면 0. */
    fun start(id: UUID, startsAt: Instant, endsAt: Instant): Int = jdbc.update(
        "UPDATE contest SET starts_at = ?, ends_at = ? WHERE id = ? AND starts_at IS NULL", Timestamp.from(startsAt), Timestamp.from(endsAt), id,
    )

    fun join(contestId: UUID, userId: String, displayName: String): Boolean = jdbc.update(
        "INSERT INTO contest_entry (contest_id, user_id, display_name) VALUES (?, ?, ?) ON CONFLICT DO NOTHING", contestId, userId, displayName,
    ) == 1

    fun entry(contestId: UUID, userId: String): Entry? =
        jdbc.query("SELECT * FROM contest_entry WHERE contest_id = ? AND user_id = ?", ENTRY, contestId, userId).firstOrNull()

    fun entries(contestId: UUID): List<Entry> =
        jdbc.query("SELECT * FROM contest_entry WHERE contest_id = ? ORDER BY joined_at", ENTRY, contestId)

    fun entryCount(contestId: UUID): Int =
        jdbc.queryForObject("SELECT count(*) FROM contest_entry WHERE contest_id = ?", Int::class.java, contestId) ?: 0

    /** 이 사람이 참가 중이고 지금 돌고 있으며 이 문제를 건 대회들. 판정 하나가 여러 대회의 점수일 수 있다. */
    fun runningFor(userId: String, problemId: String, now: Instant, kinds: Set<Contest.Kind>): List<Contest> = jdbc.query(
        """
        SELECT c.* FROM contest c
          JOIN contest_entry e ON e.contest_id = c.id AND e.user_id = ?
          JOIN contest_problem p ON p.contest_id = c.id AND p.problem_id = ?
         WHERE c.starts_at <= ? AND c.ends_at > ? AND c.kind IN (${kinds.joinToString { "'${it.name}'" }})
        """.trimIndent(),
        CONTEST, userId, problemId, Timestamp.from(now), Timestamp.from(now),
    )

    /** 반례 대전에서 과녁을 깨뜨렸다. 처음이면 true — 같은 과녁은 한 번이다. */
    fun hack(contestId: UUID, userId: String, problemId: String, target: String, at: Instant): Boolean = jdbc.update(
        "INSERT INTO contest_hack (contest_id, user_id, problem_id, target_name, at) VALUES (?, ?, ?, ?, ?) ON CONFLICT DO NOTHING",
        contestId, userId, problemId, target, Timestamp.from(at),
    ) == 1

    fun hackCount(contestId: UUID, userId: String, problemId: String): Int = jdbc.queryForObject(
        "SELECT count(*) FROM contest_hack WHERE contest_id = ? AND user_id = ? AND problem_id = ?", Int::class.java, contestId, userId, problemId,
    ) ?: 0

    /** 반례 대전의 점수: 깨뜨린 과녁의 수. 그 수에 이른 시각이 동점의 순서다. */
    fun scoreHack(contestId: UUID, userId: String, problemId: String, count: Int, at: Instant) {
        jdbc.update(
            """
            INSERT INTO contest_score (contest_id, user_id, problem_id, best_score, attempts, solved_at)
            VALUES (?, ?, ?, ?, 1, ?)
            ON CONFLICT (contest_id, user_id, problem_id) DO UPDATE SET
              best_score = EXCLUDED.best_score, attempts = contest_score.attempts + 1, solved_at = EXCLUDED.solved_at
            """.trimIndent(),
            contestId, userId, problemId, count, Timestamp.from(at),
        )
    }

    /** 점수를 올린다. 최고 점수만 남고, 처음 만점의 시각이 남는다. */
    fun score(contestId: UUID, userId: String, problemId: String, score: Int, full: Boolean, at: Instant) {
        jdbc.update(
            """
            INSERT INTO contest_score (contest_id, user_id, problem_id, best_score, attempts, solved_at)
            VALUES (?, ?, ?, ?, 1, ?)
            ON CONFLICT (contest_id, user_id, problem_id) DO UPDATE SET
              best_score = GREATEST(contest_score.best_score, EXCLUDED.best_score),
              attempts = contest_score.attempts + 1,
              solved_at = COALESCE(contest_score.solved_at, EXCLUDED.solved_at)
            """.trimIndent(),
            contestId, userId, problemId, score, if (full) Timestamp.from(at) else null,
        )
    }

    fun scores(contestId: UUID): List<Score> = jdbc.query(
        "SELECT user_id, problem_id, best_score, attempts, solved_at FROM contest_score WHERE contest_id = ?",
        { rs, _ -> Score(rs.getString(1), rs.getString(2), rs.getInt(3), rs.getInt(4), rs.getTimestamp(5)?.toInstant()) },
        contestId,
    )

    // --- 레이팅 (§8.4) ---

    /** 끝났는데 아직 레이팅을 적용하지 않은 레이팅 대회들. */
    fun finishedUnrated(now: Instant): List<Contest> = jdbc.query(
        "SELECT * FROM contest WHERE rated AND published AND rated_at IS NULL AND ends_at <= ? ORDER BY ends_at",
        CONTEST, Timestamp.from(now),
    )

    /** 적용 표시. 한 번만 — 두 스레드가 동시에 와도 하나만 1 을 받는다. */
    fun markRated(id: UUID): Int = jdbc.update("UPDATE contest SET rated_at = now() WHERE id = ? AND rated_at IS NULL", id)

    fun ratingOf(userId: String): Int = jdbc.query(
        "SELECT rating FROM user_rating WHERE user_id = ?", { rs, _ -> rs.getInt(1) }, userId,
    ).firstOrNull() ?: Elo.INITIAL

    fun ratingsOf(userIds: Collection<String>): Map<String, Int> {
        if (userIds.isEmpty()) return emptyMap()
        val marks = userIds.joinToString { "?" }
        return jdbc.query(
            "SELECT user_id, rating FROM user_rating WHERE user_id IN ($marks)",
            { rs, _ -> rs.getString(1) to rs.getInt(2) }, *userIds.toTypedArray(),
        ).toMap()
    }

    fun applyChange(contestId: UUID, userId: String, rank: Int, before: Int, after: Int) {
        jdbc.update(
            "INSERT INTO rating_change (contest_id, user_id, rank, before, after) VALUES (?, ?, ?, ?, ?) ON CONFLICT DO NOTHING",
            contestId, userId, rank, before, after,
        )
        jdbc.update(
            """
            INSERT INTO user_rating (user_id, rating, contests) VALUES (?, ?, 1)
            ON CONFLICT (user_id) DO UPDATE SET rating = EXCLUDED.rating, contests = user_rating.contests + 1
            """.trimIndent(),
            userId, after,
        )
    }

    fun changesOf(contestId: UUID): Map<String, Int> = jdbc.query(
        "SELECT user_id, after - before FROM rating_change WHERE contest_id = ?",
        { rs, _ -> rs.getString(1) to rs.getInt(2) }, contestId,
    ).toMap()

    fun rating(userId: String): Rating {
        val history = jdbc.query(
            """
            SELECT r.contest_id, c.title, r.rank, r.before, r.after, r.applied_at FROM rating_change r JOIN contest c ON c.id = r.contest_id
             WHERE r.user_id = ? ORDER BY r.applied_at DESC LIMIT 50
            """.trimIndent(),
            { rs, _ -> RatingChange(rs.getObject(1, UUID::class.java), rs.getString(2), rs.getInt(3), rs.getInt(4), rs.getInt(5), rs.getTimestamp(6).toInstant()) },
            userId,
        )
        val contests = jdbc.query("SELECT contests FROM user_rating WHERE user_id = ?", { rs, _ -> rs.getInt(1) }, userId).firstOrNull() ?: 0
        return Rating(ratingOf(userId), contests, history)
    }

    fun export(userId: String): Map<String, Any?> = mapOf(
        "rating" to rating(userId),
        "entries" to jdbc.query(
            "SELECT contest_id, display_name, joined_at FROM contest_entry WHERE user_id = ? ORDER BY joined_at",
            { rs, _ -> mapOf("contestId" to rs.getString(1), "displayName" to rs.getString(2), "joinedAt" to rs.getTimestamp(3).toInstant()) },
            userId,
        ),
        "scores" to jdbc.query(
            "SELECT contest_id, problem_id, best_score, attempts, solved_at FROM contest_score WHERE user_id = ?",
            { rs, _ ->
                mapOf("contestId" to rs.getString(1), "problemId" to rs.getString(2), "bestScore" to rs.getInt(3),
                      "attempts" to rs.getInt(4), "solvedAt" to rs.getTimestamp(5)?.toInstant())
            },
            userId,
        ),
    )

    /** 삭제 (§11.3). 순위표의 이름을 지운다. 점수는 남의 순위에 얽혀 있어 남긴다 — 이름 없는 줄이 된다. */
    fun erase(userId: String): Int {
        jdbc.update("DELETE FROM contest_hack WHERE user_id = ?", userId)
        // 레이팅 변화는 남의 변화와 얽혀 있어 남기되 이름을 지운다. 지금의 레이팅은 지운다.
        jdbc.update("UPDATE rating_change SET user_id = 'erased:' || contest_id::text WHERE user_id = ?", userId)
        jdbc.update("DELETE FROM user_rating WHERE user_id = ?", userId)
        jdbc.update("UPDATE contest_score SET user_id = 'erased:' || contest_id::text || ':' || problem_id WHERE user_id = ?", userId)
        return jdbc.update("UPDATE contest_entry SET display_name = '(지운 계정)', user_id = 'erased:' || contest_id::text WHERE user_id = ?", userId)
    }

    private companion object {
        val CONTEST = RowMapper { rs, _ ->
            Contest(
                id = rs.getObject("id", UUID::class.java),
                kind = Contest.Kind.valueOf(rs.getString("kind")),
                title = rs.getString("title"),
                createdBy = rs.getString("created_by"),
                startsAt = rs.getTimestamp("starts_at")?.toInstant(),
                endsAt = rs.getTimestamp("ends_at")?.toInstant(),
                minutes = rs.getInt("minutes").takeUnless { rs.wasNull() },
                published = rs.getBoolean("published"),
                joinCode = rs.getString("join_code"),
                parentId = rs.getObject("parent_id", UUID::class.java),
                rated = rs.getBoolean("rated"),
                ratedAt = rs.getTimestamp("rated_at")?.toInstant(),
                createdAt = rs.getTimestamp("created_at").toInstant(),
            )
        }
        val ENTRY = RowMapper { rs, _ ->
            Entry(rs.getObject("contest_id", UUID::class.java), rs.getString("user_id"), rs.getString("display_name"), rs.getTimestamp("joined_at").toInstant())
        }
    }
}
