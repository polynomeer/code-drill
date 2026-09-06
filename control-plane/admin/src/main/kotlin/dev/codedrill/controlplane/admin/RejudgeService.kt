package dev.codedrill.controlplane.admin

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

/**
 * 재채점 (기술 설계서 §8.1 rejudge_job, §11.2 2인 승인, §4.2 INV-02).
 *
 * 재채점은 이미 사용자에게 보여 준 판정을 바꾸는 행위다. 그래서 세 가지를 지킨다.
 *
 * 1. **요청과 승인을 분리한다.** 자기가 요청한 작업을 자기가 승인할 수 없다. DB
 *    제약으로도 막아 두어 애플리케이션 버그로도 우회되지 않는다.
 * 2. **종료된 제출을 덮어쓰지 않는다.** revision 을 올려 새 판정으로 기록한다 (INV-02).
 *    이전 판정이 사라지면 "왜 점수가 바뀌었나"에 답할 수 없다.
 * 3. **모든 단계를 감사 로그에 남긴다** (§13.3).
 *
 * 실제 재채점 실행은 승인 이후 아웃박스를 통해 실행 영역으로 나간다. 승인만으로 상태를
 * 바꾸지 않으므로, 승인했지만 아직 돌지 않은 구간이 상태로 드러난다.
 */
@Service
class RejudgeService(private val jdbc: JdbcTemplate, private val audit: AuditLog) {

    /** 요청. 이 시점에는 아무것도 바뀌지 않는다. */
    @Transactional
    fun request(scope: String, reason: String, actor: String): RejudgeJob {
        require(reason.isNotBlank()) { "재채점에는 사유가 필요하다 (§8.1 rejudge_job.reason)" }

        val id = UUID.randomUUID()
        jdbc.update(
            """
            INSERT INTO rejudge_job (id, scope, reason, status, requested_by)
            VALUES (?, ?, ?, ?, ?)
            """.trimIndent(),
            id, scope, reason, RejudgeStatus.REQUESTED.name, actor,
        )
        audit.record(
            AuditAction.REJUDGE_REQUESTED,
            subject = id.toString(),
            actor = actor,
            detail = mapOf("scope" to scope, "reason" to reason),
        )
        return find(id)!!
    }

    /**
     * 승인. 요청자와 다른 사람이어야 한다.
     *
     * DB 제약이 같은 것을 막지만, 여기서 먼저 걸러 사용자에게 이유를 말해 준다. 제약에만
     * 기대면 화면에는 정체 모를 500 이 뜬다.
     */
    @Transactional
    fun approve(id: UUID, approver: String): ApprovalOutcome {
        val job = find(id) ?: return ApprovalOutcome.Rejected("없는 작업이다")

        if (job.status != RejudgeStatus.REQUESTED) {
            return ApprovalOutcome.Rejected("이미 ${job.status} 인 작업이다")
        }
        if (job.requestedBy == approver) {
            return ApprovalOutcome.Rejected(
                "요청자와 승인자가 같다. 대량 재채점은 두 사람이 필요하다 (§11.2)",
            )
        }

        jdbc.update(
            """
            UPDATE rejudge_job SET status = ?, approved_by = ?, updated_at = now()
             WHERE id = ? AND status = ?
            """.trimIndent(),
            RejudgeStatus.APPROVED.name, approver, id, RejudgeStatus.REQUESTED.name,
        )
        audit.record(
            AuditAction.REJUDGE_APPROVED,
            subject = id.toString(),
            actor = approver,
            detail = mapOf("scope" to job.scope, "requestedBy" to job.requestedBy),
        )
        return ApprovalOutcome.Approved(find(id)!!)
    }

    @Transactional
    fun reject(id: UUID, actor: String, reason: String): ApprovalOutcome {
        val job = find(id) ?: return ApprovalOutcome.Rejected("없는 작업이다")
        if (job.status != RejudgeStatus.REQUESTED) {
            return ApprovalOutcome.Rejected("이미 ${job.status} 인 작업이다")
        }

        jdbc.update(
            "UPDATE rejudge_job SET status = ?, updated_at = now() WHERE id = ?",
            RejudgeStatus.REJECTED.name, id,
        )
        audit.record(
            AuditAction.REJUDGE_REJECTED,
            subject = id.toString(),
            actor = actor,
            detail = mapOf("reason" to reason),
        )
        return ApprovalOutcome.Approved(find(id)!!)
    }

    /**
     * 승인된 작업의 대상 제출 목록.
     *
     * scope 는 `problem:<id>` 또는 `submission:<uuid>` 다. 승인되지 않은 작업은 대상을
     * 돌려주지 않는다 — 조회만으로 재채점이 시작되는 경로를 만들지 않기 위해서다.
     */
    fun targets(id: UUID): List<UUID> {
        val job = find(id) ?: return emptyList()
        if (job.status != RejudgeStatus.APPROVED) return emptyList()

        val (kind, value) = job.scope.split(':', limit = 2).let { it[0] to it.getOrElse(1) { "" } }
        return when (kind) {
            "problem" -> jdbc.query(
                "SELECT id FROM submission WHERE problem_id = ? AND status = 'COMPLETED'",
                { rs, _ -> rs.getObject(1, UUID::class.java) },
                value,
            )
            "submission" -> listOf(UUID.fromString(value))
            else -> emptyList()
        }
    }

    fun find(id: UUID): RejudgeJob? =
        jdbc.query("SELECT * FROM rejudge_job WHERE id = ?", MAPPER, id).firstOrNull()

    fun recent(limit: Int): List<RejudgeJob> =
        jdbc.query("SELECT * FROM rejudge_job ORDER BY created_at DESC LIMIT ?", MAPPER, limit)

    sealed interface ApprovalOutcome {
        data class Approved(val job: RejudgeJob) : ApprovalOutcome
        data class Rejected(val reason: String) : ApprovalOutcome
    }

    private companion object {
        val MAPPER = RowMapper { rs, _ ->
            RejudgeJob(
                id = rs.getObject("id", UUID::class.java),
                scope = rs.getString("scope"),
                reason = rs.getString("reason"),
                status = RejudgeStatus.valueOf(rs.getString("status")),
                requestedBy = rs.getString("requested_by"),
                approvedBy = rs.getString("approved_by"),
                createdAt = rs.getTimestamp("created_at").toInstant(),
            )
        }
    }
}

data class RejudgeJob(
    val id: UUID,
    val scope: String,
    val reason: String,
    val status: RejudgeStatus,
    val requestedBy: String,
    val approvedBy: String?,
    val createdAt: Instant,
)

enum class RejudgeStatus { REQUESTED, APPROVED, REJECTED, RUNNING, COMPLETED }
