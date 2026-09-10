package dev.codedrill.controlplane.submission.trace

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import dev.codedrill.judge.protocol.TraceStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class DivergenceRepository(private val jdbc: JdbcTemplate, private val json: ObjectMapper) {

    /**
     * 참조 지문을 넣는다. 같은 (문제 버전, 케이스) 가 두 번 와도 하나다.
     *
     * 덮어쓰지 않는다 — 같은 문제 버전의 참조 풀이는 늘 같은 트레이스를 낸다. 두 번째가
     * 다르다면 그것은 갱신할 일이 아니라 결정성이 깨졌다는 신호이며, 덮어쓰면 그 사실이
     * 사라진다.
     */
    fun putSignature(
        problemVersionId: String,
        caseId: String,
        keys: List<String>,
        status: TraceStatus,
    ): Int = jdbc.update(
        """
        INSERT INTO reference_trace (problem_version_id, case_id, keys, status)
        VALUES (?, ?, ?::jsonb, ?)
        ON CONFLICT (problem_version_id, case_id) DO NOTHING
        """.trimIndent(),
        problemVersionId, caseId, json.writeValueAsString(keys), status.name,
    )

    fun signature(problemVersionId: String, caseId: String): ReferenceSignature? = jdbc.query(
        "SELECT keys, status FROM reference_trace WHERE problem_version_id = ? AND case_id = ?",
        { rs, _ ->
            ReferenceSignature(
                keys = json.readValue<List<String>>(rs.getString("keys")),
                status = TraceStatus.valueOf(rs.getString("status")),
            )
        },
        problemVersionId, caseId,
    ).firstOrNull()

    fun save(divergence: Divergence): Int = jdbc.update(
        """
        INSERT INTO submission_divergence
            (submission_id, case_id, outcome, shared_prefix, diverged_at_seq, source_line,
             expected_step, actual_step)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT (submission_id) DO UPDATE
           SET case_id = EXCLUDED.case_id,
               outcome = EXCLUDED.outcome,
               shared_prefix = EXCLUDED.shared_prefix,
               diverged_at_seq = EXCLUDED.diverged_at_seq,
               source_line = EXCLUDED.source_line,
               expected_step = EXCLUDED.expected_step,
               actual_step = EXCLUDED.actual_step,
               created_at = now()
        """.trimIndent(),
        divergence.submissionId, divergence.caseId, divergence.outcome.name,
        divergence.sharedPrefix, divergence.divergedAtSeq, divergence.sourceLine,
        divergence.expectedStep, divergence.actualStep,
    )

    fun find(submissionId: UUID): Divergence? =
        jdbc.query("SELECT * FROM submission_divergence WHERE submission_id = ?", MAPPER, submissionId)
            .firstOrNull()

    /** 아직 참조를 기다리는 제출들. 참조 지문이 도착하면 이들을 마저 계산한다. */
    fun pendingFor(caseId: String): List<UUID> = jdbc.query(
        "SELECT submission_id FROM submission_divergence WHERE outcome = ? AND case_id = ?",
        { rs, _ -> rs.getObject("submission_id", UUID::class.java) },
        DivergenceOutcome.PENDING.name, caseId,
    )

    private companion object {
        val MAPPER = RowMapper { rs, _ ->
            Divergence(
                submissionId = rs.getObject("submission_id", UUID::class.java),
                caseId = rs.getString("case_id"),
                outcome = DivergenceOutcome.valueOf(rs.getString("outcome")),
                sharedPrefix = rs.getObject("shared_prefix") as Int?,
                divergedAtSeq = rs.getObject("diverged_at_seq") as Long?,
                sourceLine = rs.getObject("source_line") as Int?,
                expectedStep = rs.getString("expected_step"),
                actualStep = rs.getString("actual_step"),
            )
        }
    }
}

/** 참조 풀이의 정규화 지문. 이벤트 본문은 여기 없다. */
data class ReferenceSignature(val keys: List<String>, val status: TraceStatus)
