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
            INSERT INTO discussion_post (id, problem_id, parent_id, author_id, title, body,
                anchor_submission_id, anchor_line_from, anchor_line_to, anchor_step, anchor_excerpt, spoiler, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            post.id, post.problemId, post.parentId, post.authorId, post.title, post.body,
            post.anchor?.submissionId, post.anchor?.lineFrom, post.anchor?.lineTo, post.anchor?.step, post.anchor?.excerpt,
            post.spoiler, post.status.name,
        )
    }

    fun find(id: UUID): DiscussionPost? =
        jdbc.query("SELECT * FROM discussion_post WHERE id = ?", POST, id).firstOrNull()

    /** 한 문제의 질문들. 내려진 것은 뺀다. 최근 것부터. */
    fun questions(problemId: String, limit: Int): List<DiscussionPost> = jdbc.query(
        """
        SELECT * FROM discussion_post
         WHERE problem_id = ? AND parent_id IS NULL AND status = 'VISIBLE'
         ORDER BY created_at DESC LIMIT ?
        """.trimIndent(),
        POST, problemId, limit,
    )

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
            SELECT id, problem_id, parent_id, title, body, anchor_submission_id, anchor_line_from, anchor_line_to,
                   anchor_step, spoiler, status, created_at
              FROM discussion_post WHERE author_id = ? ORDER BY created_at
            """.trimIndent(),
            { rs, _ ->
                mapOf(
                    "id" to rs.getString(1), "problemId" to rs.getString(2), "parentId" to rs.getString(3),
                    "title" to rs.getString(4), "body" to rs.getString(5), "anchorSubmissionId" to rs.getString(6),
                    "anchorLineFrom" to rs.getObject(7), "anchorLineTo" to rs.getObject(8), "anchorStep" to rs.getObject(9),
                    "spoiler" to rs.getBoolean(10), "status" to rs.getString(11), "createdAt" to rs.getTimestamp(12).toInstant(),
                )
            },
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
