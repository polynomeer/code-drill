package dev.codedrill.controlplane.workspace

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import dev.codedrill.judge.protocol.MutantOutcome
import dev.codedrill.platform.messaging.OutboxEvent
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

/**
 * 변이 평가 저장소.
 *
 * [insertWithOutbox] 가 행과 메시지를 함께 넣는 이유는 [TrialRepository] 와 같다 —
 * 하나만 나가면 사용자는 영원히 PENDING 을 보거나, 결과가 도착해도 놓을 자리가 없다.
 */
@Repository
class MutationRepository(private val jdbc: JdbcTemplate, private val json: ObjectMapper) {

    fun insertWithOutbox(evaluation: MutationEvaluation, event: OutboxEvent): Int {
        val inserted = jdbc.update(
            """
            INSERT INTO mutation_evaluation
                (id, user_id, problem_id, problem_version, cases, status)
            VALUES (?, ?, ?, ?, ?::jsonb, ?)
            """.trimIndent(),
            evaluation.id, evaluation.userId, evaluation.problemId, evaluation.problemVersion,
            json.writeValueAsString(evaluation.cases), evaluation.status.name,
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

    /** **PENDING 일 때만 적는다.** 아웃박스는 at-least-once 라 같은 결과가 두 번 온다. */
    fun complete(
        id: UUID,
        status: MutationRunStatus,
        message: String?,
        mistakenCases: List<Int>,
        outcomes: List<MutantOutcome>,
    ): Int = jdbc.update(
        """
        UPDATE mutation_evaluation
           SET status = ?, message = ?, mistaken_cases = ?::jsonb, outcomes = ?::jsonb,
               completed_at = now()
         WHERE id = ? AND status = ?
        """.trimIndent(),
        status.name, message, json.writeValueAsString(mistakenCases),
        json.writeValueAsString(outcomes), id, MutationRunStatus.PENDING.name,
    )

    fun find(id: UUID): MutationEvaluation? =
        jdbc.query("SELECT * FROM mutation_evaluation WHERE id = ?", mapper(), id).firstOrNull()

    /** 최근 한 시간 동안 이 사용자가 돌린 횟수. 쿼터가 이 값을 본다 (§10.2). */
    fun recentCount(userId: String, since: Instant): Int = jdbc.queryForObject(
        "SELECT count(*) FROM mutation_evaluation WHERE user_id = ? AND created_at > ?",
        Int::class.java, userId, java.sql.Timestamp.from(since),
    ) ?: 0

    /** 개인 데이터 반출 (§11.3). 케이스는 사용자가 적은 것이므로 사용자의 것이다. */
    fun export(userId: String): List<Map<String, Any?>> = jdbc.query(
        """
        SELECT id, problem_id, cases, status, mistaken_cases, outcomes, created_at
          FROM mutation_evaluation WHERE user_id = ? ORDER BY created_at
        """.trimIndent(),
        { rs, _ ->
            mapOf(
                "id" to rs.getString("id"),
                "problemId" to rs.getString("problem_id"),
                "cases" to rs.getString("cases"),
                "status" to rs.getString("status"),
                "mistakenCases" to rs.getString("mistaken_cases"),
                "outcomes" to rs.getString("outcomes"),
                "createdAt" to rs.getTimestamp("created_at")?.toInstant(),
            )
        },
        userId,
    )

    /** 개인 데이터 삭제 (§11.3). 사용자가 적은 케이스만 비운다 — 횟수는 용량 계획의 근거다. */
    fun erase(userId: String): Int = jdbc.update(
        "UPDATE mutation_evaluation SET cases = NULL WHERE user_id = ?",
        userId,
    )

    private fun mapper() = RowMapper { rs, _ ->
        MutationEvaluation(
            id = rs.getObject("id", UUID::class.java),
            userId = rs.getString("user_id"),
            problemId = rs.getString("problem_id"),
            problemVersion = rs.getInt("problem_version"),
            cases = rs.getString("cases")?.let { json.readValue(it) } ?: emptyList(),
            status = MutationRunStatus.valueOf(rs.getString("status")),
            message = rs.getString("message"),
            mistakenCases = rs.getString("mistaken_cases")?.let { json.readValue<List<Int>>(it) }
                ?: emptyList(),
            outcomes = rs.getString("outcomes")?.let { json.readValue<List<MutantOutcome>>(it) }
                ?: emptyList(),
            createdAt = rs.getTimestamp("created_at").toInstant(),
        )
    }
}
