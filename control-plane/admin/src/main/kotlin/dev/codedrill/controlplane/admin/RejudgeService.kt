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
 * 승인과 실행을 또 한 번 나눈다. 승인만으로 아무것도 돌지 않으므로, "승인했지만 아직
 * 돌지 않은" 구간이 상태로 드러난다. 되돌릴 수 없는 일은 한 번의 클릭으로 시작되지
 * 않아야 한다.
 *
 * dry-run 은 판정을 바꾸지 않고 **무엇이 바뀔지만** 본다 (§19.2 FR-721). 테스트 데이터를
 * 고치기 전에 몇 명의 점수가 움직이는지 알 수 있어야, 고칠지 말지를 근거로 정한다.
 */
@Service
class RejudgeService(
    private val jdbc: JdbcTemplate,
    private val audit: AuditLog,
    private val submissions: JudgedSubmissions,
) {

    /** 요청. 이 시점에는 아무것도 바뀌지 않는다. */
    @Transactional
    fun request(scope: String, reason: String, actor: String, dryRun: Boolean = false): RejudgeJob {
        require(reason.isNotBlank()) { "재채점에는 사유가 필요하다 (§8.1 rejudge_job.reason)" }

        val id = UUID.randomUUID()
        jdbc.update(
            """
            INSERT INTO rejudge_job (id, scope, reason, status, requested_by, dry_run)
            VALUES (?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            id, scope, reason, RejudgeStatus.REQUESTED.name, actor, dryRun,
        )
        audit.record(
            AuditAction.REJUDGE_REQUESTED,
            subject = id.toString(),
            actor = actor,
            detail = mapOf("scope" to scope, "reason" to reason, "dryRun" to dryRun),
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
        if (job.status == RejudgeStatus.REQUESTED || job.status == RejudgeStatus.REJECTED) {
            return emptyList()
        }
        return targetsOf(job)
    }

    /**
     * scope 가 가리키는 종료된 제출.
     *
     * 제출 테이블은 제출 도메인의 것이므로 직접 읽지 않는다 (§3.1). Admin 이 정하는 것은
     * **무엇을 다시 돌릴지**이고, 그것이 무엇인지 아는 쪽은 제출 도메인이다.
     */
    private fun targetsOf(job: RejudgeJob): List<UUID> {
        val (kind, value) = job.scope.split(':', limit = 2).let { it[0] to it.getOrElse(1) { "" } }
        return when (kind) {
            "problem" -> submissions.completedFor(value)
            "submission" -> runCatching { UUID.fromString(value) }
                .map(submissions::completed)
                .getOrDefault(emptyList())
            else -> emptyList()
        }
    }

    /**
     * 실행. 승인된 작업의 대상을 실제로 채점 큐에 올린다 (§3.2).
     *
     * 대상 표와 아웃박스 이벤트가 한 트랜잭션에 함께 커밋된다. 둘이 갈라지면 "돌린 줄
     * 알았는데 안 돈" 대상이나 "표에 없는데 도는" 대상이 생기고, 어느 쪽도 나중에
     * 알아낼 방법이 없다.
     *
     * 이미 실행한 작업은 다시 실행하지 않는다. 대량 재채점을 두 번 돌리면 사용자 점수가
     * 두 번 흔들린다.
     */
    @Transactional
    fun dispatch(id: UUID, actor: String): DispatchOutcome {
        val job = find(id) ?: return DispatchOutcome.Rejected("없는 작업이다")
        if (job.status != RejudgeStatus.APPROVED) {
            return DispatchOutcome.Rejected("승인된 작업만 실행할 수 있다. 지금은 ${job.status} 다")
        }

        val ids = targetsOf(job)
        if (ids.isEmpty()) return DispatchOutcome.Rejected("대상이 없다: ${job.scope}")

        ids.forEach { submissionId ->
            jdbc.update(
                """
                INSERT INTO rejudge_target (job_id, submission_id) VALUES (?, ?)
                ON CONFLICT (job_id, submission_id) DO NOTHING
                """.trimIndent(),
                id, submissionId,
            )
        }
        val queued = submissions.requeue(ids)

        jdbc.update(
            """
            UPDATE rejudge_job
               SET status = ?, dispatched_at = now(), target_count = ?, updated_at = now()
             WHERE id = ?
            """.trimIndent(),
            RejudgeStatus.RUNNING.name, queued, id,
        )
        audit.record(
            AuditAction.REJUDGE_DISPATCHED,
            subject = id.toString(),
            actor = actor,
            detail = mapOf("scope" to job.scope, "targets" to queued, "dryRun" to job.dryRun),
        )
        return DispatchOutcome.Dispatched(find(id)!!, queued)
    }

    // --- 제출 도메인이 판정을 확정할 때 묻는 것 ---
    //
    // 제출 모듈의 포트를 여기서 직접 구현하지 않는다. 그러면 Admin 이 제출 모듈을
    // 참조하게 되고, 도메인 모듈이 서로를 모른다는 §3.1 이 깨진다. 두 타입을 잇는
    // 어댑터는 조립 지점인 :control-plane:app 에 있다.

    /**
     * 이 제출이 기다리고 있는 재채점.
     *
     * 아직 결과가 오지 않은 대상만 본다. 이미 끝난 대상까지 보면, 사용자가 그 뒤에
     * 새로 제출한 판정이 지나간 재채점의 결과로 기록된다.
     */
    fun pendingFor(submissionId: UUID): PendingRejudge? = jdbc.query(
        """
        SELECT j.id, j.dry_run FROM rejudge_target t
          JOIN rejudge_job j ON j.id = t.job_id
         WHERE t.submission_id = ? AND t.completed_at IS NULL
         ORDER BY t.dispatched_at DESC LIMIT 1
        """.trimIndent(),
        { rs, _ ->
            PendingRejudge(
                jobId = rs.getObject("id", UUID::class.java),
                dryRun = rs.getBoolean("dry_run"),
            )
        },
        submissionId,
    ).firstOrNull()

    @Transactional
    fun judged(outcome: RejudgeResult) {
        jdbc.update(
            """
            UPDATE rejudge_target SET completed_at = now()
             WHERE job_id = ? AND submission_id = ? AND completed_at IS NULL
            """.trimIndent(),
            outcome.jobId, outcome.submissionId,
        )

        // 바뀌지 않은 판정까지 감사에 남기면 로그가 재채점 대상 수만큼 부풀고, 정작
        // 봐야 할 변경이 묻힌다. 변경만 남긴다 (§13.3 판정 변경).
        if (outcome.changed) {
            audit.record(
                AuditAction.JUDGEMENT_REVISED,
                subject = outcome.submissionId.toString(),
                actor = "rejudge:${outcome.jobId}",
                detail = mapOf(
                    "applied" to outcome.applied,
                    "from" to "${outcome.previousVerdict}(${outcome.previousScore})",
                    "to" to "${outcome.verdict}(${outcome.score})",
                ),
            )
        }

        // 남은 대상이 없으면 작업이 끝난 것이다.
        val remaining = jdbc.queryForObject(
            "SELECT count(*) FROM rejudge_target WHERE job_id = ? AND completed_at IS NULL",
            Int::class.java, outcome.jobId,
        ) ?: 0
        if (remaining == 0) {
            jdbc.update(
                "UPDATE rejudge_job SET status = ?, updated_at = now() WHERE id = ? AND status = ?",
                RejudgeStatus.COMPLETED.name, outcome.jobId, RejudgeStatus.RUNNING.name,
            )
        }
    }

    /**
     * 진행 상황과 결과 요약.
     *
     * dry-run 이든 아니든 같은 표를 낸다 — 다른 것은 "반영했는가"뿐이고, 물어보는
     * 사람이 알고 싶은 것은 **무엇이 바뀌었나**로 같다.
     */
    fun report(id: UUID): RejudgeReport? {
        val job = find(id) ?: return null

        val done = jdbc.queryForObject(
            "SELECT count(*) FROM rejudge_target WHERE job_id = ? AND completed_at IS NOT NULL",
            Int::class.java, id,
        ) ?: 0

        val changes = jdbc.query(
            """
            SELECT g.submission_id, g.verdict, g.score, s.revision,
                   previous.verdict AS previous_verdict, previous.score AS previous_score
              FROM submission_judgement g
              JOIN submission s ON s.id = g.submission_id
              LEFT JOIN LATERAL (
                  SELECT p.verdict, p.score FROM submission_judgement p
                   WHERE p.submission_id = g.submission_id AND p.id < g.id
                   ORDER BY p.id DESC LIMIT 1
              ) previous ON true
             WHERE g.rejudge_job_id = ?
               AND (previous.verdict IS DISTINCT FROM g.verdict
                    OR previous.score IS DISTINCT FROM g.score)
             ORDER BY g.id
            """.trimIndent(),
            { rs, _ ->
                VerdictChange(
                    submissionId = rs.getObject("submission_id", UUID::class.java).toString(),
                    from = "${rs.getString("previous_verdict")}(${rs.getObject("previous_score")})",
                    to = "${rs.getString("verdict")}(${rs.getInt("score")})",
                )
            },
            id,
        )

        return RejudgeReport(
            job = job,
            completed = done,
            changes = changes,
        )
    }

    fun find(id: UUID): RejudgeJob? =
        jdbc.query("SELECT * FROM rejudge_job WHERE id = ?", MAPPER, id).firstOrNull()

    fun recent(limit: Int): List<RejudgeJob> =
        jdbc.query("SELECT * FROM rejudge_job ORDER BY created_at DESC LIMIT ?", MAPPER, limit)

    sealed interface DispatchOutcome {
        data class Dispatched(val job: RejudgeJob, val targets: Int) : DispatchOutcome
        data class Rejected(val reason: String) : DispatchOutcome
    }

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
                dryRun = rs.getBoolean("dry_run"),
                targetCount = rs.getInt("target_count"),
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
    /** 판정을 바꾸지 않고 무엇이 바뀔지만 본다 (§19.2 FR-721). */
    val dryRun: Boolean,
    val targetCount: Int,
    val createdAt: Instant,
)

/** 이 제출이 기다리고 있는 재채점. */
data class PendingRejudge(val jobId: UUID, val dryRun: Boolean)

/**
 * 재채점이 무엇을 무엇으로 바꿨는지.
 *
 * 바뀌지 않은 것도 함께 온다. "재채점했는데 아무것도 안 바뀌었다"는 결과 자체가 답이며,
 * 감사에서 가장 자주 필요한 사실이다.
 */
data class RejudgeResult(
    val submissionId: UUID,
    val jobId: UUID,
    val applied: Boolean,
    val previousVerdict: String?,
    val previousScore: Int?,
    val verdict: String,
    val score: Int,
) {
    val changed: Boolean get() = previousVerdict != verdict || previousScore != score
}

/** 재채점이 무엇을 바꿨는지. dry-run 이면 "바꿨을" 것들이다. */
data class RejudgeReport(
    val job: RejudgeJob,
    val completed: Int,
    val changes: List<VerdictChange>,
)

data class VerdictChange(val submissionId: String, val from: String, val to: String)

enum class RejudgeStatus { REQUESTED, APPROVED, REJECTED, RUNNING, COMPLETED }
