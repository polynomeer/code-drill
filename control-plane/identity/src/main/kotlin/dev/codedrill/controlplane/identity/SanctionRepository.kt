package dev.codedrill.controlplane.identity

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

@Repository
class SanctionRepository(private val jdbc: JdbcTemplate) {

    fun insert(s: Sanction) {
        jdbc.update(
            """
            INSERT INTO sanction (id, user_id, kind, reason, evidence, issued_by, starts_at, ends_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            s.id, s.userId, s.kind.name, s.reason, s.evidence, s.issuedBy, Timestamp.from(s.startsAt), s.endsAt?.let { Timestamp.from(it) },
        )
    }

    fun find(id: UUID): Sanction? = jdbc.query("SELECT * FROM sanction WHERE id = ?", SANCTION, id).firstOrNull()

    /** 이 사람의 제재 전부. 최근 것부터. */
    fun of(userId: String): List<Sanction> =
        jdbc.query("SELECT * FROM sanction WHERE user_id = ? ORDER BY created_at DESC", SANCTION, userId)

    /** 지금 효력이 있는 것 중 가장 무거운 것. 문 앞에서 매 요청마다 묻는다 — 색인이 있어야 한다. */
    fun activeOf(userId: String, now: Instant): Sanction? = jdbc.query(
        """
        SELECT * FROM sanction
         WHERE user_id = ? AND kind <> 'WARNING' AND lifted_at IS NULL AND starts_at <= ? AND (ends_at IS NULL OR ends_at > ?)
         ORDER BY CASE kind WHEN 'SUSPEND' THEN 0 ELSE 1 END, created_at DESC LIMIT 1
        """.trimIndent(),
        SANCTION, userId, Timestamp.from(now), Timestamp.from(now),
    ).firstOrNull()

    fun lift(id: UUID, by: String): Int =
        jdbc.update("UPDATE sanction SET lifted_by = ?, lifted_at = now() WHERE id = ? AND lifted_at IS NULL", by, id)

    /** 이의는 한 번. 이미 냈으면 0. */
    fun appeal(id: UUID, userId: String, text: String): Int = jdbc.update(
        "UPDATE sanction SET appeal = ?, appealed_at = now() WHERE id = ? AND user_id = ? AND appeal IS NULL", text, id, userId,
    )

    fun openAppeals(): List<Sanction> = jdbc.query(
        "SELECT * FROM sanction WHERE appeal IS NOT NULL AND appeal_resolution IS NULL ORDER BY appealed_at", SANCTION,
    )

    fun resolveAppeal(id: UUID, resolution: AppealResolution, by: String, note: String?): Int = jdbc.update(
        """
        UPDATE sanction SET appeal_resolution = ?, appeal_note = ?, resolved_by = ?, resolved_at = now()
         WHERE id = ? AND appeal IS NOT NULL AND appeal_resolution IS NULL
        """.trimIndent(),
        resolution.name, note, by, id,
    )

    fun export(userId: String): List<Map<String, Any?>> = of(userId).map {
        mapOf(
            "id" to it.id.toString(), "kind" to it.kind.name, "reason" to it.reason, "startsAt" to it.startsAt,
            "endsAt" to it.endsAt, "liftedAt" to it.liftedAt, "appeal" to it.appeal, "appealResolution" to it.appealResolution?.name,
            "appealNote" to it.appealNote, "createdAt" to it.createdAt,
        )
    }

    /** 삭제 (§11.3). 제재는 운영 기록이라 남기되, 누구의 것인지는 지운다. 이의의 글은 본인의 것이라 지운다. */
    fun erase(userId: String): Int = jdbc.update(
        "UPDATE sanction SET user_id = 'erased:' || id::text, appeal = CASE WHEN appeal IS NULL THEN NULL ELSE '' END WHERE user_id = ?",
        userId,
    )

    private companion object {
        val SANCTION = RowMapper { rs, _ ->
            Sanction(
                id = rs.getObject("id", UUID::class.java),
                userId = rs.getString("user_id"),
                kind = SanctionKind.valueOf(rs.getString("kind")),
                reason = rs.getString("reason"),
                evidence = rs.getString("evidence"),
                issuedBy = rs.getString("issued_by"),
                startsAt = rs.getTimestamp("starts_at").toInstant(),
                endsAt = rs.getTimestamp("ends_at")?.toInstant(),
                liftedBy = rs.getString("lifted_by"),
                liftedAt = rs.getTimestamp("lifted_at")?.toInstant(),
                appeal = rs.getString("appeal"),
                appealedAt = rs.getTimestamp("appealed_at")?.toInstant(),
                appealResolution = rs.getString("appeal_resolution")?.let { AppealResolution.valueOf(it) },
                appealNote = rs.getString("appeal_note"),
                resolvedBy = rs.getString("resolved_by"),
                resolvedAt = rs.getTimestamp("resolved_at")?.toInstant(),
                createdAt = rs.getTimestamp("created_at").toInstant(),
            )
        }
    }
}
