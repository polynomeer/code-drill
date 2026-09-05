package dev.codedrill.controlplane.submission

import dev.codedrill.judge.protocol.Verdict
import dev.codedrill.platform.messaging.OutboxEvent
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * 제출과 아웃박스 저장소.
 *
 * [insertWithOutbox] 는 이름 그대로 둘을 함께 넣는다. 호출부의 트랜잭션 안에서만
 * 의미가 있으며, 도메인 행만 커밋되고 이벤트가 빠지는 경우를 구조적으로 막는다 (§3.2).
 */
@Repository
class SubmissionRepository(private val jdbc: JdbcTemplate) {

    fun insertWithOutbox(submission: Submission, event: OutboxEvent): Int {
        val inserted = jdbc.update(
            """
            INSERT INTO submission (
                id, user_id, idempotency_key, problem_id, problem_version,
                language, source, status, version
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0)
            ON CONFLICT (user_id, idempotency_key) DO NOTHING
            """.trimIndent(),
            submission.id, submission.userId, submission.idempotencyKey,
            submission.problemId, submission.problemVersion,
            submission.language, "", submission.status.name,
        )
        // 멱등 키가 겹쳤다면 이미 만들어진 제출이 있다. 이벤트를 또 넣지 않는다.
        if (inserted == 0) return 0

        jdbc.update(
            """
            INSERT INTO outbox_event (id, aggregate, aggregate_id, type, payload, occurred_at)
            VALUES (?, ?, ?, ?, ?::jsonb, ?)
            """.trimIndent(),
            event.id, event.aggregate, event.aggregateId, event.type, event.payload, java.sql.Timestamp.from(event.occurredAt),
        )
        return 1
    }

    /** 트레이스를 붙인다. 판정 상태는 건드리지 않는다 (§7.1 판정과 독립). */
    fun attachTrace(id: UUID, traceJson: String): Int =
        jdbc.update("UPDATE submission SET trace = ?::jsonb WHERE id = ?", traceJson, id)

    fun findTrace(id: UUID): String? =
        jdbc.query("SELECT trace FROM submission WHERE id = ?", { rs, _ -> rs.getString(1) }, id)
            .firstOrNull()

    fun updateSource(id: UUID, source: String) {
        jdbc.update("UPDATE submission SET source = ? WHERE id = ?", source, id)
    }

    fun findById(id: UUID): Submission? =
        jdbc.query("SELECT * FROM submission WHERE id = ?", MAPPER, id).firstOrNull()

    fun findSource(id: UUID): String? =
        jdbc.query("SELECT source FROM submission WHERE id = ?", { rs, _ -> rs.getString(1) }, id)
            .firstOrNull()

    fun findByIdempotencyKey(userId: String, key: String): Submission? =
        jdbc.query(
            "SELECT * FROM submission WHERE user_id = ? AND idempotency_key = ?",
            MAPPER, userId, key,
        ).firstOrNull()

    fun recentFor(userId: String, limit: Int): List<Submission> =
        jdbc.query(
            "SELECT * FROM submission WHERE user_id = ? ORDER BY created_at DESC LIMIT ?",
            MAPPER, userId, limit,
        )

    /**
     * 상태를 낙관적으로 옮긴다 (§3.2).
     *
     * 현재 상태와 version 을 함께 조건에 넣으므로, 두 소비자가 같은 전이를 시도하면
     * 하나만 성공한다. 0 을 돌려주면 호출부는 이미 처리된 것으로 보고 조용히 끝낸다.
     */
    fun transition(id: UUID, from: SubmissionStatus, to: SubmissionStatus, version: Long): Int =
        jdbc.update(
            """
            UPDATE submission
               SET status = ?, version = version + 1, updated_at = now()
             WHERE id = ? AND status = ? AND version = ?
            """.trimIndent(),
            to.name, id, from.name, version,
        )

    fun complete(
        id: UUID,
        verdict: Verdict,
        score: Int,
        compileLog: String?,
        groupsJson: String,
    ): Int = jdbc.update(
        """
        UPDATE submission
           SET status = ?, verdict = ?, score = ?, compile_log = ?, groups = ?::jsonb,
               version = version + 1, updated_at = now()
         WHERE id = ? AND status <> ?
        """.trimIndent(),
        SubmissionStatus.COMPLETED.name, verdict.name, score, compileLog, groupsJson,
        id, SubmissionStatus.COMPLETED.name,
    )

    private companion object {
        val MAPPER = RowMapper { rs, _ ->
            Submission(
                id = rs.getObject("id", UUID::class.java),
                userId = rs.getString("user_id"),
                idempotencyKey = rs.getString("idempotency_key"),
                problemId = rs.getString("problem_id"),
                problemVersion = rs.getInt("problem_version"),
                language = rs.getString("language"),
                status = SubmissionStatus.valueOf(rs.getString("status")),
                verdict = rs.getString("verdict")?.let(Verdict::valueOf),
                score = rs.getObject("score") as? Int,
                compileLog = rs.getString("compile_log"),
                groupsJson = rs.getString("groups"),
                version = rs.getLong("version"),
                createdAt = rs.getTimestamp("created_at").toInstant(),
            )
        }
    }
}
