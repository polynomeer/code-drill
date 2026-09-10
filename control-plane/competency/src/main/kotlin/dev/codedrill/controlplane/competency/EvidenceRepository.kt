package dev.codedrill.controlplane.competency

import dev.codedrill.platform.problempackage.Competency
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * 증거 저장소.
 *
 * **고치는 메서드가 없다.** 넣고 읽을 뿐이다 — Competency 모듈은 원본 증거를 수정하지
 * 않는다 (§3.1). 코드에 그 경로를 만들지 않아야 실수로도 우회하지 못한다. 감사 로그를
 * append-only 로 둔 것과 같은 이유다.
 */
@Repository
class EvidenceRepository(private val jdbc: JdbcTemplate) {

    /**
     * 증거를 넣는다. 같은 사건이 두 번 오면 아무 일도 하지 않는다.
     *
     * 아웃박스도 재채점도 at-least-once 다. 막지 않으면 한 번의 판정이 증거 두 개가 되고,
     * 숙련도가 실제보다 단단해 보인다.
     */
    fun insert(evidence: Evidence): Int = jdbc.update(
        """
        INSERT INTO competency_evidence
            (id, user_id, competency, source, success, weight, problem_id, reference, detail, occurred_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT DO NOTHING
        """.trimIndent(),
        evidence.id, evidence.userId, evidence.competency.name, evidence.source.name,
        evidence.success, evidence.weight, evidence.problemId, evidence.reference,
        evidence.detail, java.sql.Timestamp.from(evidence.occurredAt),
    )

    fun of(userId: String): List<Evidence> = jdbc.query(
        "SELECT * FROM competency_evidence WHERE user_id = ? ORDER BY occurred_at",
        MAPPER, userId,
    )

    /** 한 역량의 근거 목록. 화면이 여기서 문제와 제출로 내려간다 (FR-806). */
    fun of(userId: String, competency: Competency, limit: Int): List<Evidence> = jdbc.query(
        """
        SELECT * FROM competency_evidence
         WHERE user_id = ? AND competency = ?
         ORDER BY occurred_at DESC LIMIT ?
        """.trimIndent(),
        MAPPER, userId, competency.name, limit,
    )

    /**
     * 개인 데이터 삭제 (§11.3).
     *
     * 사람을 가리키는 값만 지운다 — 증거 자체는 문제의 통계이고, 계정 삭제는 익명화이지
     * 이력 말소가 아니다 (IdentityService.deleteAccount). 다만 원본을 가리키는 참조는
     * 지운다: 그것을 따라가면 지운 제출에 닿는다.
     */
    fun erase(userId: String): Int = jdbc.update(
        "UPDATE competency_evidence SET reference = NULL, detail = NULL WHERE user_id = ?",
        userId,
    )

    fun export(userId: String): List<Map<String, Any?>> = jdbc.query(
        """
        SELECT competency, source, success, weight, problem_id, detail, occurred_at
          FROM competency_evidence WHERE user_id = ? ORDER BY occurred_at
        """.trimIndent(),
        { rs, _ ->
            mapOf(
                "competency" to rs.getString("competency"),
                "source" to rs.getString("source"),
                "success" to rs.getBoolean("success"),
                "weight" to rs.getDouble("weight"),
                "problemId" to rs.getString("problem_id"),
                "detail" to rs.getString("detail"),
                "occurredAt" to rs.getTimestamp("occurred_at")?.toInstant(),
            )
        },
        userId,
    )

    private companion object {
        val MAPPER = RowMapper { rs, _ ->
            Evidence(
                id = rs.getObject("id", UUID::class.java),
                userId = rs.getString("user_id"),
                competency = Competency.valueOf(rs.getString("competency")),
                source = EvidenceSource.valueOf(rs.getString("source")),
                success = rs.getBoolean("success"),
                weight = rs.getDouble("weight"),
                problemId = rs.getString("problem_id"),
                reference = rs.getString("reference"),
                detail = rs.getString("detail"),
                occurredAt = rs.getTimestamp("occurred_at").toInstant(),
            )
        }
    }
}
