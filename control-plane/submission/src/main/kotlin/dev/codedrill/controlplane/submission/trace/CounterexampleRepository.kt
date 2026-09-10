package dev.codedrill.controlplane.submission.trace

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import dev.codedrill.platform.messaging.OutboxEvent
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class CounterexampleRepository(private val jdbc: JdbcTemplate, private val json: ObjectMapper) {

    /**
     * 자리를 잡고 작업을 함께 건다.
     *
     * 이미 있으면 아무것도 하지 않고 0 을 돌려준다 — 사용자가 두 번 눌렀을 때 가장 비싼
     * 작업을 두 번 돌리지 않기 위해서다.
     */
    fun startWithOutbox(submissionId: UUID, userId: String, event: OutboxEvent): Int {
        val inserted = jdbc.update(
            """
            INSERT INTO counterexample (submission_id, user_id, status)
            VALUES (?, ?, ?)
            ON CONFLICT (submission_id) DO NOTHING
            """.trimIndent(),
            submissionId, userId, CounterexampleStatus.PENDING.name,
        )
        if (inserted == 0) return 0

        jdbc.update(
            """
            INSERT INTO outbox_event (id, aggregate, aggregate_id, type, payload, occurred_at)
            VALUES (?, ?, ?, ?, ?::jsonb, ?)
            """.trimIndent(),
            event.id, event.aggregate, event.aggregateId, event.type, event.payload,
            java.sql.Timestamp.from(event.occurredAt),
        )
        return inserted
    }

    /** **PENDING 일 때만 적는다.** 아웃박스는 at-least-once 라 같은 결과가 두 번 온다. */
    fun complete(submissionId: UUID, result: Counterexample): Int = jdbc.update(
        """
        UPDATE counterexample
           SET status = ?, message = ?, args = ?::jsonb, actual = ?, expected = ?,
               original_size = ?, minimal_size = ?, rounds = ?, completed_at = now()
         WHERE submission_id = ? AND status = ?
        """.trimIndent(),
        result.status.name, result.message,
        result.args?.let { json.writeValueAsString(it) },
        result.actual, result.expected, result.originalSize, result.minimalSize, result.rounds,
        submissionId, CounterexampleStatus.PENDING.name,
    )

    fun find(submissionId: UUID): Counterexample? =
        jdbc.query("SELECT * FROM counterexample WHERE submission_id = ?", mapper(), submissionId)
            .firstOrNull()

    /** 최근 한 시간 동안 이 사용자가 돌린 횟수. 쿼터가 이 값을 본다 (§10.2). */
    fun recentCount(userId: String, since: java.time.Instant): Int = jdbc.queryForObject(
        "SELECT count(*) FROM counterexample WHERE user_id = ? AND created_at > ?",
        Int::class.java, userId, java.sql.Timestamp.from(since),
    ) ?: 0

    private fun mapper() = RowMapper { rs, _ ->
        Counterexample(
            submissionId = rs.getObject("submission_id", UUID::class.java),
            userId = rs.getString("user_id"),
            status = CounterexampleStatus.valueOf(rs.getString("status")),
            message = rs.getString("message"),
            args = rs.getString("args")?.let { json.readValue<List<Any>>(it) },
            actual = rs.getString("actual"),
            expected = rs.getString("expected"),
            originalSize = rs.getObject("original_size") as Int?,
            minimalSize = rs.getObject("minimal_size") as Int?,
            rounds = rs.getObject("rounds") as Int?,
            createdAt = rs.getTimestamp("created_at").toInstant(),
        )
    }
}
