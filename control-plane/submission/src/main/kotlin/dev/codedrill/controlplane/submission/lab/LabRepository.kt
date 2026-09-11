package dev.codedrill.controlplane.submission.lab

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import dev.codedrill.judge.protocol.ApproachResult
import dev.codedrill.platform.messaging.OutboxEvent
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
class LabRepository(private val jdbc: JdbcTemplate, private val json: ObjectMapper) {

    fun unlock(userId: String, problemId: String): Int = jdbc.update(
        "INSERT INTO editorial_unlock (user_id, problem_id) VALUES (?, ?) ON CONFLICT DO NOTHING",
        userId, problemId,
    )

    fun unlocked(userId: String, problemId: String): Boolean = jdbc.queryForObject(
        "SELECT count(*) FROM editorial_unlock WHERE user_id = ? AND problem_id = ?",
        Int::class.java, userId, problemId,
    ) == 1

    fun insertWithOutbox(run: LabRun, event: OutboxEvent) {
        jdbc.update(
            """
            INSERT INTO lab_run (id, user_id, problem_id, args, labels, status)
            VALUES (?, ?, ?, ?::jsonb, ?::jsonb, ?)
            """.trimIndent(),
            run.id, run.userId, run.problemId, json.writeValueAsString(run.args),
            json.writeValueAsString(run.labels), run.status.name,
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

    fun complete(id: UUID, results: List<ApproachResult>): Int = jdbc.update(
        """
        UPDATE lab_run SET status = ?, results = ?::jsonb, completed_at = now()
         WHERE id = ? AND status = ?
        """.trimIndent(),
        LabStatus.COMPLETED.name, json.writeValueAsString(results), id, LabStatus.PENDING.name,
    )

    fun find(id: UUID): LabRun? =
        jdbc.query("SELECT * FROM lab_run WHERE id = ?", mapper(), id).firstOrNull()

    fun recentCount(userId: String, since: Instant): Int = jdbc.queryForObject(
        "SELECT count(*) FROM lab_run WHERE user_id = ? AND created_at > ?",
        Int::class.java, userId, java.sql.Timestamp.from(since),
    ) ?: 0

    /** 개인 데이터 (§11.3). 열람 기록과 실험 입력은 사용자의 것이다. */
    fun export(userId: String): Map<String, Any?> = mapOf(
        "unlocks" to jdbc.query(
            "SELECT problem_id, unlocked_at FROM editorial_unlock WHERE user_id = ?",
            { rs, _ -> mapOf("problemId" to rs.getString(1), "at" to rs.getTimestamp(2).toInstant()) },
            userId,
        ),
        "labs" to jdbc.query(
            "SELECT id, problem_id, args, labels, status, created_at FROM lab_run WHERE user_id = ? ORDER BY created_at",
            { rs, _ ->
                mapOf(
                    "id" to rs.getString(1), "problemId" to rs.getString(2), "args" to rs.getString(3),
                    "labels" to rs.getString(4), "status" to rs.getString(5),
                    "createdAt" to rs.getTimestamp(6).toInstant(),
                )
            },
            userId,
        ),
    )

    /**
     * 삭제 (§11.3). 실험 결과는 지우고 열람 기록은 남긴다.
     *
     * 열람 기록은 역량 증거의 무게를 정한 근거다 — 지우면 그 뒤의 증거가 왜 가벼운지
     * 말할 수 없게 된다. 사전 질문의 근거를 비우되 답은 남기는 것과 같은 결이다.
     */
    fun erase(userId: String): Map<String, Int> = mapOf(
        "labs" to jdbc.update("DELETE FROM lab_run WHERE user_id = ?", userId),
    )

    private fun mapper() = RowMapper { rs, _ ->
        LabRun(
            id = rs.getObject("id", UUID::class.java),
            userId = rs.getString("user_id"),
            problemId = rs.getString("problem_id"),
            args = json.readValue(rs.getString("args")),
            labels = json.readValue(rs.getString("labels")),
            status = LabStatus.valueOf(rs.getString("status")),
            results = rs.getString("results")?.let { json.readValue<List<ApproachResult>>(it) } ?: emptyList(),
            createdAt = rs.getTimestamp("created_at").toInstant(),
        )
    }
}

/**
 * 실험실 실행 한 건 (§6.4~6.6).
 *
 * 결과에 참조 풀이의 이벤트가 통째로 실린다. 이 행이 만들어지는 조건 — 맞혔거나 해설을
 * 명시적으로 열었거나 — 은 [LabService] 한 곳이 지킨다.
 */
data class LabRun(
    val id: UUID,
    val userId: String,
    val problemId: String,
    val args: List<Any>,
    val labels: List<String>,
    val status: LabStatus,
    val results: List<ApproachResult>,
    val createdAt: Instant,
)

enum class LabStatus { PENDING, COMPLETED }
