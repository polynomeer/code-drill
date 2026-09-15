package dev.codedrill.controlplane.workspace

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.util.UUID

@Repository
class DiscussionRepository(private val jdbc: JdbcTemplate) {

    fun insert(post: DiscussionPost) {
        jdbc.update(
            """
            INSERT INTO discussion_post (id, problem_id, kind, parent_id, author_id, title, body,
                anchor_submission_id, anchor_line_from, anchor_line_to, anchor_step, anchor_excerpt, spoiler, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            post.id, post.problemId, post.kind.name, post.parentId, post.authorId, post.title, post.body,
            post.anchor?.submissionId, post.anchor?.lineFrom, post.anchor?.lineTo, post.anchor?.step, post.anchor?.excerpt,
            post.spoiler, post.status.name,
        )
    }

    fun find(id: UUID): DiscussionPost? =
        jdbc.query("SELECT * FROM discussion_post WHERE id = ?", POST, id).firstOrNull()

    /** 한 문제의 질문들. 내려진 것은 뺀다. 최근 것부터. */
    fun questions(problemId: String, limit: Int): List<DiscussionPost> = byKind(problemId, PostKind.QUESTION, limit)

    /** 한 문제의 공유된 풀이들. 내려진 것은 뺀다. */
    fun solutions(problemId: String, limit: Int): List<DiscussionPost> = byKind(problemId, PostKind.SOLUTION, limit)

    private fun byKind(problemId: String, kind: PostKind, limit: Int): List<DiscussionPost> = jdbc.query(
        """
        SELECT * FROM discussion_post
         WHERE problem_id = ? AND kind = ? AND status = 'VISIBLE'
         ORDER BY created_at DESC LIMIT ?
        """.trimIndent(),
        POST, problemId, kind.name, limit,
    )

    /** 이 사람이 이 제출로 이미 올린 풀이. 한 제출은 한 번만 올린다. */
    fun solutionOf(submissionId: UUID): DiscussionPost? = jdbc.query(
        "SELECT * FROM discussion_post WHERE kind = 'SOLUTION' AND anchor_submission_id = ? AND status = 'VISIBLE'",
        POST, submissionId,
    ).firstOrNull()

    // --- 도움됐다 (§8.5 평판) ---

    /** 한 사람이 한 글에 한 번. 두 번째는 false. */
    fun markHelpful(postId: UUID, userId: String): Boolean = jdbc.update(
        "INSERT INTO discussion_helpful (post_id, user_id) VALUES (?, ?) ON CONFLICT DO NOTHING", postId, userId,
    ) == 1

    fun helpfulCounts(postIds: Collection<UUID>): Map<UUID, Int> {
        if (postIds.isEmpty()) return emptyMap()
        val marks = postIds.joinToString { "?" }
        return jdbc.query(
            "SELECT post_id, count(*) FROM discussion_helpful WHERE post_id IN ($marks) GROUP BY post_id",
            { rs, _ -> rs.getObject(1, UUID::class.java) to rs.getInt(2) },
            *postIds.toTypedArray(),
        ).toMap()
    }

    fun helpfulBy(userId: String, postIds: Collection<UUID>): Set<UUID> {
        if (postIds.isEmpty()) return emptySet()
        val marks = postIds.joinToString { "?" }
        return jdbc.query(
            "SELECT post_id FROM discussion_helpful WHERE user_id = ? AND post_id IN ($marks)",
            { rs, _ -> rs.getObject(1, UUID::class.java) }, userId, *postIds.toTypedArray(),
        ).toSet()
    }

    /** 글쓴이별 기여의 세 항. 도움됐다는 **보이는 글**에 남은 것만 센다 — 내려진 글의 도움은 사라진다. */
    fun contributionsOf(userId: String): Triple<Int, Int, Int> {
        val helpful = jdbc.queryForObject(
            """
            SELECT count(*) FROM discussion_helpful h JOIN discussion_post p ON p.id = h.post_id
             WHERE p.author_id = ? AND p.status = 'VISIBLE'
            """.trimIndent(),
            Int::class.java, userId,
        ) ?: 0
        val solutions = jdbc.queryForObject(
            "SELECT count(*) FROM discussion_post WHERE author_id = ? AND kind = 'SOLUTION' AND status = 'VISIBLE'",
            Int::class.java, userId,
        ) ?: 0
        val answers = jdbc.queryForObject(
            "SELECT count(*) FROM discussion_post WHERE author_id = ? AND kind = 'ANSWER' AND status = 'VISIBLE'",
            Int::class.java, userId,
        ) ?: 0
        return Triple(helpful, solutions, answers)
    }

    /** 여러 글쓴이의 도움됐다 수 — 글에 실을 등급을 위한 것. */
    fun helpfulReceivedBy(authorIds: Collection<String>): Map<String, Int> {
        if (authorIds.isEmpty()) return emptyMap()
        val marks = authorIds.joinToString { "?" }
        return jdbc.query(
            """
            SELECT p.author_id, count(*) FROM discussion_helpful h JOIN discussion_post p ON p.id = h.post_id
             WHERE p.author_id IN ($marks) AND p.status = 'VISIBLE' GROUP BY p.author_id
            """.trimIndent(),
            { rs, _ -> rs.getString(1) to rs.getInt(2) }, *authorIds.toTypedArray(),
        ).toMap()
    }

    /** 한 질문의 답들. 내려진 것은 뺀다. 오래된 것부터 — 대화의 순서다. */
    fun answers(parentId: UUID): List<DiscussionPost> = jdbc.query(
        "SELECT * FROM discussion_post WHERE parent_id = ? AND status = 'VISIBLE' ORDER BY created_at",
        POST, parentId,
    )

    /** 질문별 보이는 답의 수. 목록에 "답 3" 을 적기 위한 것이다. */
    fun answerCounts(parentIds: Collection<UUID>): Map<UUID, Int> {
        if (parentIds.isEmpty()) return emptyMap()
        val marks = parentIds.joinToString { "?" }
        return jdbc.query(
            "SELECT parent_id, count(*) FROM discussion_post WHERE parent_id IN ($marks) AND status = 'VISIBLE' GROUP BY parent_id",
            { rs, _ -> rs.getObject(1, UUID::class.java) to rs.getInt(2) },
            *parentIds.toTypedArray(),
        ).toMap()
    }

    /** 내린다. VISIBLE 인 것만 바뀐다 — 두 검수자가 동시에 눌러도 한 결정만 남는다. */
    fun hide(id: UUID, reviewer: String, reason: String): Int = jdbc.update(
        """
        UPDATE discussion_post SET status = 'HIDDEN', hidden_by = ?, hidden_at = now(), hidden_reason = ?
         WHERE id = ? AND status = 'VISIBLE'
        """.trimIndent(),
        reviewer, reason, id,
    )

    /**
     * 이 제출을 붙인 보이는 글들의 (문제, 풀이 노출 여부). 제출 도메인이 "남의 제출을 읽어도
     * 되나"를 물을 때 쓴다 — 글에 붙은 리플레이 시점은 그 글을 볼 수 있는 사람이 열 수 있어야 한다.
     */
    fun anchoring(submissionId: UUID): List<Pair<String, Boolean>> = jdbc.query(
        "SELECT problem_id, spoiler FROM discussion_post WHERE anchor_submission_id = ? AND status = 'VISIBLE'",
        { rs, _ -> rs.getString(1) to rs.getBoolean(2) }, submissionId,
    )

    fun insertReport(report: DiscussionReport): Boolean = jdbc.update(
        """
        INSERT INTO discussion_report (id, post_id, reporter_id, reason, status) VALUES (?, ?, ?, ?, ?)
        ON CONFLICT (post_id, reporter_id) DO NOTHING
        """.trimIndent(),
        report.id, report.postId, report.reporterId, report.reason, report.status.name,
    ) == 1

    fun findReport(id: UUID): DiscussionReport? =
        jdbc.query("SELECT * FROM discussion_report WHERE id = ?", REPORT, id).firstOrNull()

    fun openReports(): List<DiscussionReport> =
        jdbc.query("SELECT * FROM discussion_report WHERE status = 'OPEN' ORDER BY created_at", REPORT)

    /** 한 글의 열린 신고 전부를 같은 결정으로 닫는다. */
    fun resolveReports(postId: UUID, status: ReportStatus, reviewer: String, resolution: String): Int = jdbc.update(
        """
        UPDATE discussion_report SET status = ?, resolved_by = ?, resolved_at = now(), resolution = ?
         WHERE post_id = ? AND status = 'OPEN'
        """.trimIndent(),
        status.name, reviewer, resolution, postId,
    )

    fun export(userId: String): Map<String, Any?> = mapOf(
        "posts" to jdbc.query(
            """
            SELECT id, problem_id, kind, parent_id, title, body, anchor_submission_id, anchor_line_from, anchor_line_to,
                   anchor_step, spoiler, status, created_at
              FROM discussion_post WHERE author_id = ? ORDER BY created_at
            """.trimIndent(),
            { rs, _ ->
                mapOf(
                    "id" to rs.getString(1), "problemId" to rs.getString(2), "kind" to rs.getString(3), "parentId" to rs.getString(4),
                    "title" to rs.getString(5), "body" to rs.getString(6), "anchorSubmissionId" to rs.getString(7),
                    "anchorLineFrom" to rs.getObject(8), "anchorLineTo" to rs.getObject(9), "anchorStep" to rs.getObject(10),
                    "spoiler" to rs.getBoolean(11), "status" to rs.getString(12), "createdAt" to rs.getTimestamp(13).toInstant(),
                )
            },
            userId,
        ),
        "helpful" to jdbc.query(
            "SELECT post_id, created_at FROM discussion_helpful WHERE user_id = ? ORDER BY created_at",
            { rs, _ -> mapOf("postId" to rs.getString(1), "createdAt" to rs.getTimestamp(2).toInstant()) },
            userId,
        ),
        "reports" to jdbc.query(
            "SELECT id, post_id, reason, status, created_at FROM discussion_report WHERE reporter_id = ? ORDER BY created_at",
            { rs, _ ->
                mapOf(
                    "id" to rs.getString(1), "postId" to rs.getString(2), "reason" to rs.getString(3),
                    "status" to rs.getString(4), "createdAt" to rs.getTimestamp(5).toInstant(),
                )
            },
            userId,
        ),
    )

    /**
     * 삭제 (§11.3). 글은 그 사람이 쓴 내용이라 본문과 코드 구간을 비우고, 누구의 것인지를
     * 지운다. 글 자체는 남긴다 — 답이 달린 질문을 지우면 남의 답이 허공에 뜬다.
     */
    fun erase(userId: String): Int {
        jdbc.update("UPDATE discussion_report SET reporter_id = 'erased:' || id::text WHERE reporter_id = ?", userId)
        // 남긴 도움됐다는 지운다 — 누가 남겼는지가 곧 그 사람이다. 받은 것은 글에 남는다.
        jdbc.update("DELETE FROM discussion_helpful WHERE user_id = ?", userId)
        return jdbc.update(
            """
            UPDATE discussion_post SET author_id = NULL, body = '', title = CASE WHEN title IS NULL THEN NULL ELSE '(지운 계정의 질문)' END,
                   anchor_submission_id = NULL, anchor_line_from = NULL, anchor_line_to = NULL, anchor_step = NULL, anchor_excerpt = NULL
             WHERE author_id = ?
            """.trimIndent(),
            userId,
        )
    }

    private companion object {
        val POST = RowMapper { rs, _ ->
            DiscussionPost(
                id = rs.getObject("id", UUID::class.java),
                problemId = rs.getString("problem_id"),
                kind = PostKind.valueOf(rs.getString("kind")),
                parentId = rs.getObject("parent_id", UUID::class.java),
                authorId = rs.getString("author_id"),
                title = rs.getString("title"),
                body = rs.getString("body"),
                anchor = rs.getObject("anchor_submission_id", UUID::class.java)?.let { submissionId ->
                    Anchor(
                        submissionId = submissionId,
                        lineFrom = rs.intOrNull("anchor_line_from"),
                        lineTo = rs.intOrNull("anchor_line_to"),
                        step = rs.intOrNull("anchor_step"),
                        excerpt = rs.getString("anchor_excerpt"),
                    )
                },
                spoiler = rs.getBoolean("spoiler"),
                status = PostStatus.valueOf(rs.getString("status")),
                hiddenBy = rs.getString("hidden_by"),
                hiddenAt = rs.getTimestamp("hidden_at")?.toInstant(),
                hiddenReason = rs.getString("hidden_reason"),
                createdAt = rs.getTimestamp("created_at").toInstant(),
            )
        }
        val REPORT = RowMapper { rs, _ ->
            DiscussionReport(
                id = rs.getObject("id", UUID::class.java),
                postId = rs.getObject("post_id", UUID::class.java),
                reporterId = rs.getString("reporter_id"),
                reason = rs.getString("reason"),
                status = ReportStatus.valueOf(rs.getString("status")),
                resolvedBy = rs.getString("resolved_by"),
                resolvedAt = rs.getTimestamp("resolved_at")?.toInstant(),
                resolution = rs.getString("resolution"),
                createdAt = rs.getTimestamp("created_at").toInstant(),
            )
        }

        fun ResultSet.intOrNull(column: String): Int? = getInt(column).takeUnless { wasNull() }
    }
}
