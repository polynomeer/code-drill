package dev.codedrill.controlplane.coaching

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class TransferRepository(private val jdbc: JdbcTemplate) {

    fun insert(task: TransferTask): Int = jdbc.update(
        """
        INSERT INTO transfer_task
            (id, session_id, user_id, source_problem_id, target_problem_id, status, created_at)
        VALUES (?, ?, ?, ?, ?, ?, ?)
        """.trimIndent(),
        task.id, task.sessionId, task.userId, task.sourceProblemId, task.targetProblemId,
        task.status.name, java.sql.Timestamp.from(task.createdAt),
    )

    fun explain(id: UUID, explanation: String): Int = jdbc.update(
        """
        UPDATE transfer_task SET explanation = ?, status = ?
         WHERE id = ? AND status = ?
        """.trimIndent(),
        explanation, TransferStatus.EXPLAINED.name, id, TransferStatus.ASSIGNED.name,
    )

    fun complete(id: UUID, status: TransferStatus, helpLevel: Int): Int = jdbc.update(
        """
        UPDATE transfer_task SET status = ?, help_level = ?, completed_at = now()
         WHERE id = ? AND completed_at IS NULL
        """.trimIndent(),
        status.name, helpLevel, id,
    )

    fun find(id: UUID): TransferTask? =
        jdbc.query("SELECT * FROM transfer_task WHERE id = ?", MAPPER, id).firstOrNull()

    fun ofSession(sessionId: UUID): TransferTask? = jdbc.query(
        "SELECT * FROM transfer_task WHERE session_id = ? ORDER BY created_at DESC LIMIT 1",
        MAPPER, sessionId,
    ).firstOrNull()

    /**
     * 이 사람에게 이 문제로 걸려 있는, 아직 안 끝난 과제.
     *
     * 판정이 끝날 때마다 묻는다. 여러 개가 걸려 있으면 **가장 오래된 것**을 먼저 닫는다 —
     * 먼저 받은 과제가 먼저 끝나는 것이 사람이 기대하는 순서다.
     */
    fun pending(userId: String, targetProblemId: String): TransferTask? = jdbc.query(
        """
        SELECT * FROM transfer_task
         WHERE user_id = ? AND target_problem_id = ? AND completed_at IS NULL
         ORDER BY created_at LIMIT 1
        """.trimIndent(),
        MAPPER, userId, targetProblemId,
    ).firstOrNull()

    /** 이미 이 문제를 변형 과제로 받은 적이 있는지. 같은 문제를 두 번 주지 않는다. */
    fun assignedTargets(userId: String): Set<String> = jdbc.query(
        "SELECT DISTINCT target_problem_id FROM transfer_task WHERE user_id = ?",
        { rs, _ -> rs.getString("target_problem_id") },
        userId,
    ).toSet()

    /** 개인 데이터 반출 (§11.3). 설명은 사용자가 쓴 것이다. */
    fun export(userId: String): List<Map<String, Any?>> = jdbc.query(
        """
        SELECT id, source_problem_id, target_problem_id, explanation, status, help_level,
               created_at, completed_at
          FROM transfer_task WHERE user_id = ? ORDER BY created_at
        """.trimIndent(),
        { rs, _ ->
            mapOf(
                "id" to rs.getString("id"),
                "sourceProblemId" to rs.getString("source_problem_id"),
                "targetProblemId" to rs.getString("target_problem_id"),
                "explanation" to rs.getString("explanation"),
                "status" to rs.getString("status"),
                "helpLevel" to rs.getObject("help_level"),
                "createdAt" to rs.getTimestamp("created_at")?.toInstant(),
                "completedAt" to rs.getTimestamp("completed_at")?.toInstant(),
            )
        },
        userId,
    )

    private companion object {
        val MAPPER = RowMapper { rs, _ ->
            TransferTask(
                id = rs.getObject("id", UUID::class.java),
                sessionId = rs.getObject("session_id", UUID::class.java),
                userId = rs.getString("user_id"),
                sourceProblemId = rs.getString("source_problem_id"),
                targetProblemId = rs.getString("target_problem_id"),
                explanation = rs.getString("explanation"),
                status = TransferStatus.valueOf(rs.getString("status")),
                helpLevel = rs.getObject("help_level") as Int?,
                createdAt = rs.getTimestamp("created_at").toInstant(),
                completedAt = rs.getTimestamp("completed_at")?.toInstant(),
            )
        }
    }
}
