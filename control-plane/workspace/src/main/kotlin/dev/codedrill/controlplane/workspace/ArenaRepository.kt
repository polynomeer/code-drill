package dev.codedrill.controlplane.workspace

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import dev.codedrill.judge.protocol.ArenaResult
import dev.codedrill.platform.messaging.OutboxEvent
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
class ArenaRepository(private val jdbc: JdbcTemplate, private val json: ObjectMapper) {

    fun insertWithOutbox(attempt: ArenaAttempt, event: OutboxEvent) {
        jdbc.update(
            "INSERT INTO arena_attempt (id, user_id, problem_id, args, status) VALUES (?, ?, ?, ?::jsonb, ?)",
            attempt.id, attempt.userId, attempt.problemId, json.writeValueAsString(attempt.args), attempt.status.name,
        )
        jdbc.update(
            """
            INSERT INTO outbox_event (id, aggregate, aggregate_id, type, payload, occurred_at)
            VALUES (?, ?, ?, ?, ?::jsonb, ?)
            """.trimIndent(),
            event.id, event.aggregate, event.aggregateId, event.type, event.payload,
            java.sql.Timestamp.from(event.occurredAt),
        )
    }

    fun complete(id: UUID, status: ArenaAttemptStatus, message: String?, results: List<ArenaResult>): Int =
        jdbc.update(
            """
            UPDATE arena_attempt SET status = ?, message = ?, results = ?::jsonb, completed_at = now()
             WHERE id = ? AND status = ?
            """.trimIndent(),
            status.name, message, json.writeValueAsString(results), id, ArenaAttemptStatus.PENDING.name,
        )

    fun find(id: UUID): ArenaAttempt? =
        jdbc.query("SELECT * FROM arena_attempt WHERE id = ?", mapper(), id).firstOrNull()

    fun recentCount(userId: String, since: Instant): Int = jdbc.queryForObject(
        "SELECT count(*) FROM arena_attempt WHERE user_id = ? AND created_at > ?",
        Int::class.java, userId, java.sql.Timestamp.from(since),
    ) ?: 0

    /**
     * 깨뜨린 기록을 반영한다.
     *
     * 사람 단위로 한 번만 센다 — 같은 오답을 열 번 깨뜨려도 한 사람이다. 크기는 작아질
     * 때만 갱신한다. 처음 깨뜨린 사람은 한 번 정해지면 바뀌지 않는다.
     */
    fun recordBreak(problemId: String, mutantName: String, userId: String, size: Int) {
        jdbc.update(
            """
            INSERT INTO arena_breaker (problem_id, mutant_name, user_id, best_size) VALUES (?, ?, ?, ?)
            ON CONFLICT (problem_id, mutant_name, user_id)
            DO UPDATE SET best_size = LEAST(arena_breaker.best_size, EXCLUDED.best_size)
            """.trimIndent(),
            problemId, mutantName, userId, size,
        )
        jdbc.update(
            """
            INSERT INTO arena_record (problem_id, mutant_name, breakers, smallest_size, smallest_by, first_broken_by, first_broken_at)
            VALUES (?, ?, 1, ?, ?, ?, now())
            ON CONFLICT (problem_id, mutant_name) DO UPDATE SET
                breakers = (SELECT count(*) FROM arena_breaker b WHERE b.problem_id = EXCLUDED.problem_id AND b.mutant_name = EXCLUDED.mutant_name),
                smallest_size = CASE WHEN arena_record.smallest_size IS NULL OR EXCLUDED.smallest_size < arena_record.smallest_size
                                     THEN EXCLUDED.smallest_size ELSE arena_record.smallest_size END,
                smallest_by = CASE WHEN arena_record.smallest_size IS NULL OR EXCLUDED.smallest_size < arena_record.smallest_size
                                   THEN EXCLUDED.smallest_by ELSE arena_record.smallest_by END
            """.trimIndent(),
            problemId, mutantName, size, userId, userId,
        )
    }

    fun records(problemId: String, userId: String): List<ArenaRecord> = jdbc.query(
        """
        SELECT r.mutant_name, r.breakers, r.smallest_size, r.smallest_by, r.first_broken_by, b.best_size
          FROM arena_record r
          LEFT JOIN arena_breaker b ON b.problem_id = r.problem_id AND b.mutant_name = r.mutant_name AND b.user_id = ?
         WHERE r.problem_id = ?
        """.trimIndent(),
        { rs, _ ->
            ArenaRecord(
                mutantName = rs.getString("mutant_name"),
                breakers = rs.getInt("breakers"),
                smallestSize = rs.getObject("smallest_size") as Int?,
                smallestIsMine = rs.getString("smallest_by") == userId,
                firstIsMine = rs.getString("first_broken_by") == userId,
                myBest = rs.getObject("best_size") as Int?,
            )
        },
        userId, problemId,
    )

    fun export(userId: String): List<Map<String, Any?>> = jdbc.query(
        "SELECT id, problem_id, args, status, results, created_at FROM arena_attempt WHERE user_id = ? ORDER BY created_at",
        { rs, _ ->
            mapOf(
                "id" to rs.getString(1), "problemId" to rs.getString(2), "args" to rs.getString(3),
                "status" to rs.getString(4), "results" to rs.getString(5), "createdAt" to rs.getTimestamp(6).toInstant(),
            )
        },
        userId,
    )

    /** 삭제 (§11.3). 입력은 비우고 기록판의 "누가"는 익명으로 남긴다. */
    fun erase(userId: String): Int {
        jdbc.update("UPDATE arena_record SET smallest_by = NULL WHERE smallest_by = ?", userId)
        jdbc.update("UPDATE arena_record SET first_broken_by = NULL WHERE first_broken_by = ?", userId)
        return jdbc.update("UPDATE arena_attempt SET args = NULL, results = NULL WHERE user_id = ?", userId)
    }

    private fun mapper() = RowMapper { rs, _ ->
        ArenaAttempt(
            id = rs.getObject("id", UUID::class.java),
            userId = rs.getString("user_id"),
            problemId = rs.getString("problem_id"),
            args = rs.getString("args")?.let { json.readValue(it) } ?: emptyList(),
            status = ArenaAttemptStatus.valueOf(rs.getString("status")),
            message = rs.getString("message"),
            results = rs.getString("results")?.let { json.readValue<List<ArenaResult>>(it) } ?: emptyList(),
            createdAt = rs.getTimestamp("created_at").toInstant(),
        )
    }
}
