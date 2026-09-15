package dev.codedrill.controlplane.integrity

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class IntegrityRepository(private val jdbc: JdbcTemplate) {

    fun savePrint(submissionId: UUID, userId: String, problemId: String, language: String, print: Fingerprint.Print) {
        jdbc.update(
            """
            INSERT INTO submission_fingerprint (submission_id, user_id, problem_id, language, token_count, hashes)
            VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT (submission_id) DO NOTHING
            """.trimIndent(),
            submissionId, userId, problemId, language, print.tokenCount, print.hashes.toTypedArray(),
        )
    }

    /** 같은 문제·같은 언어의 다른 사람 지문. 최근 것부터 [limit] 개 — 비교는 선형이라 상한이 있어야 한다. */
    fun others(problemId: String, language: String, userId: String, limit: Int): List<StoredPrint> = jdbc.query(
        """
        SELECT submission_id, user_id, hashes FROM submission_fingerprint
         WHERE problem_id = ? AND language = ? AND (user_id IS NULL OR user_id <> ?)
         ORDER BY created_at DESC LIMIT ?
        """.trimIndent(),
        { rs, _ ->
            @Suppress("UNCHECKED_CAST")
            val hashes = (rs.getArray("hashes").array as Array<Int>).toSet()
            StoredPrint(rs.getObject("submission_id", UUID::class.java), rs.getString("user_id"), hashes)
        },
        problemId, language, userId, limit,
    )

    fun insertFlag(flag: SimilarityFlag): Boolean = jdbc.update(
        """
        INSERT INTO similarity_flag (id, problem_id, language, submission_id, other_submission_id, user_id, other_user_id, score, status)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT (submission_id, other_submission_id) DO NOTHING
        """.trimIndent(),
        flag.id, flag.problemId, flag.language, flag.submissionId, flag.otherSubmissionId,
        flag.userId, flag.otherUserId, flag.score, flag.status.name,
    ) == 1

    fun find(id: UUID): SimilarityFlag? = jdbc.query("SELECT * FROM similarity_flag WHERE id = ?", FLAG, id).firstOrNull()

    fun open(limit: Int): List<SimilarityFlag> =
        jdbc.query("SELECT * FROM similarity_flag WHERE status = 'OPEN' ORDER BY score DESC, created_at LIMIT ?", FLAG, limit)

    /** 결정. OPEN 인 것만 바뀐다 — 두 검수자가 동시에 눌러도 한 결정만 남는다. */
    fun resolve(id: UUID, status: FlagStatus, reviewer: String, note: String?): Int = jdbc.update(
        """
        UPDATE similarity_flag SET status = ?, reviewed_by = ?, reviewed_at = now(), note = ?
         WHERE id = ? AND status = 'OPEN'
        """.trimIndent(),
        status.name, reviewer, note, id,
    )

    fun export(userId: String): Map<String, Any?> = mapOf(
        "flags" to jdbc.query(
            """
            SELECT id, problem_id, language, score, status, created_at FROM similarity_flag
             WHERE user_id = ? OR other_user_id = ? ORDER BY created_at
            """.trimIndent(),
            { rs, _ ->
                mapOf(
                    "id" to rs.getString(1), "problemId" to rs.getString(2), "language" to rs.getString(3),
                    "score" to rs.getDouble(4), "status" to rs.getString(5), "createdAt" to rs.getTimestamp(6).toInstant(),
                )
            },
            userId, userId,
        ),
    )

    /**
     * 삭제 (§11.3). 지문은 지운다 — 소스에서 뜬 것이라 소스와 함께 간다. 신호는 남되
     * 누구의 것인지는 지운다: 상대방 쪽의 기록이기도 하기 때문이다.
     */
    fun erase(userId: String): Int {
        jdbc.update("UPDATE similarity_flag SET user_id = NULL WHERE user_id = ?", userId)
        jdbc.update("UPDATE similarity_flag SET other_user_id = NULL WHERE other_user_id = ?", userId)
        return jdbc.update("DELETE FROM submission_fingerprint WHERE user_id = ?", userId)
    }

    private companion object {
        val FLAG = RowMapper { rs, _ ->
            SimilarityFlag(
                id = rs.getObject("id", UUID::class.java),
                problemId = rs.getString("problem_id"),
                language = rs.getString("language"),
                submissionId = rs.getObject("submission_id", UUID::class.java),
                otherSubmissionId = rs.getObject("other_submission_id", UUID::class.java),
                userId = rs.getString("user_id"),
                otherUserId = rs.getString("other_user_id"),
                score = rs.getDouble("score"),
                status = FlagStatus.valueOf(rs.getString("status")),
                reviewedBy = rs.getString("reviewed_by"),
                reviewedAt = rs.getTimestamp("reviewed_at")?.toInstant(),
                note = rs.getString("note"),
                createdAt = rs.getTimestamp("created_at").toInstant(),
            )
        }
    }
}
