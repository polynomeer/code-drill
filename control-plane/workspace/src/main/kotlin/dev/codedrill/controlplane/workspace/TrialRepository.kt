package dev.codedrill.controlplane.workspace

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import dev.codedrill.platform.messaging.OutboxEvent
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

/**
 * 시험 실행 저장소.
 *
 * [insertWithOutbox] 는 이름 그대로 둘을 함께 넣는다. 행만 남고 메시지가 안 나가면 사용자는
 * 영원히 PENDING 을 보고, 메시지만 나가고 행이 없으면 결과가 도착해도 놓을 자리가 없다.
 * 제출이 같은 이유로 같은 모양을 쓴다 (§3.2).
 */
@Repository
class TrialRepository(private val jdbc: JdbcTemplate, private val json: ObjectMapper) {

    fun insertWithOutbox(trial: TrialRun, source: String, event: OutboxEvent): Int {
        val inserted = jdbc.update(
            """
            INSERT INTO trial_run
                (id, user_id, problem_id, problem_version, language, source, cases, status)
            VALUES (?, ?, ?, ?, ?, ?, ?::jsonb, ?)
            """.trimIndent(),
            trial.id, trial.userId, trial.problemId, trial.problemVersion, trial.language,
            source, json.writeValueAsString(trial.cases), trial.status.name,
        )
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

    /**
     * 결과를 적는다.
     *
     * **PENDING 일 때만 적는다.** 아웃박스는 at-least-once 라 같은 실행이 두 번 도착할 수
     * 있는데, 두 번째를 그대로 덮어쓰면 사용자가 보던 결과가 이유 없이 새로 고쳐진다.
     */
    fun complete(
        id: UUID,
        status: TrialStatus,
        compileLog: String?,
        results: List<TrialCaseResult>,
    ): Int = jdbc.update(
        """
        UPDATE trial_run
           SET status = ?, compile_log = ?, results = ?::jsonb, completed_at = now()
         WHERE id = ? AND status = ?
        """.trimIndent(),
        status.name, compileLog, json.writeValueAsString(results), id, TrialStatus.PENDING.name,
    )

    fun find(id: UUID): TrialRun? =
        jdbc.query("SELECT * FROM trial_run WHERE id = ?", mapper(), id).firstOrNull()

    /** 최근 한 시간 동안 이 사용자가 돌린 횟수. 쿼터가 이 값을 본다 (§10.2). */
    fun recentCount(userId: String, since: Instant): Int = jdbc.queryForObject(
        "SELECT count(*) FROM trial_run WHERE user_id = ? AND created_at > ?",
        Int::class.java, userId, java.sql.Timestamp.from(since),
    ) ?: 0

    /**
     * 개인 데이터 반출 (§11.3).
     *
     * 소스와 입력을 함께 낸다. 사용자가 적은 것이므로 사용자의 것이다.
     */
    fun export(userId: String): List<Map<String, Any?>> = jdbc.query(
        """
        SELECT id, problem_id, language, source, cases, status, results, created_at
          FROM trial_run WHERE user_id = ? ORDER BY created_at
        """.trimIndent(),
        { rs, _ ->
            mapOf(
                "id" to rs.getString("id"),
                "problemId" to rs.getString("problem_id"),
                "language" to rs.getString("language"),
                "source" to rs.getString("source"),
                "cases" to rs.getString("cases"),
                "status" to rs.getString("status"),
                "results" to rs.getString("results"),
                "createdAt" to rs.getTimestamp("created_at")?.toInstant(),
            )
        },
        userId,
    )

    /**
     * 개인 데이터 삭제 (§11.3).
     *
     * 행을 지우지 않고 사용자가 적은 것만 비운다. 실행 횟수는 용량 계획의 근거이고,
     * 그것까지 지워 달라고 요청받은 것은 아니다.
     */
    fun erase(userId: String): Int = jdbc.update(
        "UPDATE trial_run SET source = NULL, cases = NULL, results = NULL WHERE user_id = ?",
        userId,
    )

    private fun mapper() = RowMapper { rs, _ ->
        TrialRun(
            id = rs.getObject("id", UUID::class.java),
            userId = rs.getString("user_id"),
            problemId = rs.getString("problem_id"),
            problemVersion = rs.getInt("problem_version"),
            language = rs.getString("language"),
            cases = rs.getString("cases")?.let { json.readValue(it) } ?: emptyList(),
            status = TrialStatus.valueOf(rs.getString("status")),
            compileLog = rs.getString("compile_log"),
            results = rs.getString("results")?.let { json.readValue<List<TrialCaseResult>>(it) }
                ?: emptyList(),
            createdAt = rs.getTimestamp("created_at").toInstant(),
        )
    }
}
