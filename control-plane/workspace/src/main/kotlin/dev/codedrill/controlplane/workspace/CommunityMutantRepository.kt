package dev.codedrill.controlplane.workspace

import dev.codedrill.platform.problempackage.DefectKind
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
class CommunityMutantRepository(private val jdbc: JdbcTemplate) {

    fun insert(donation: ArenaDonation) {
        jdbc.update(
            """
            INSERT INTO arena_donation (id, problem_id, submission_id, donor_user_id, source, note, status)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            donation.id, donation.problemId, donation.submissionId, donation.donorUserId,
            donation.source, donation.note, donation.status.name,
        )
    }

    fun find(id: UUID): ArenaDonation? =
        jdbc.query("SELECT * FROM arena_donation WHERE id = ?", DONATION, id).firstOrNull()

    fun findBySubmission(submissionId: UUID): ArenaDonation? =
        jdbc.query("SELECT * FROM arena_donation WHERE submission_id = ?", DONATION, submissionId).firstOrNull()

    fun approved(problemId: String): List<ArenaDonation> = jdbc.query(
        "SELECT * FROM arena_donation WHERE problem_id = ? AND status = 'APPROVED' AND source IS NOT NULL ORDER BY created_at",
        DONATION, problemId,
    )

    fun mine(userId: String, problemId: String): List<ArenaDonation> = jdbc.query(
        "SELECT * FROM arena_donation WHERE donor_user_id = ? AND problem_id = ? ORDER BY created_at DESC",
        DONATION, userId, problemId,
    )

    fun pending(): List<ArenaDonation> =
        jdbc.query("SELECT * FROM arena_donation WHERE status = 'PENDING' ORDER BY created_at", DONATION)

    /** 검수 결정. PENDING 인 것만 바뀐다 — 두 검수자가 동시에 눌러도 한 결정만 남는다. */
    fun review(id: UUID, status: DonationStatus, kind: DefectKind?, reviewer: String, reason: String?): Int =
        jdbc.update(
            """
            UPDATE arena_donation SET status = ?, kind = ?, reviewed_by = ?, reviewed_at = now(), reason = ?
             WHERE id = ? AND status = 'PENDING'
            """.trimIndent(),
            status.name, kind?.name, reviewer, reason, id,
        )

    /** 세워진 것을 내린다. APPROVED 인 것만. */
    fun retire(id: UUID, reviewer: String, reason: String): Int = jdbc.update(
        """
        UPDATE arena_donation SET status = 'RETIRED', reviewed_by = ?, reviewed_at = now(), reason = ?
         WHERE id = ? AND status = 'APPROVED'
        """.trimIndent(),
        reviewer, reason, id,
    )

    fun insertReport(report: ArenaReport): Boolean = jdbc.update(
        """
        INSERT INTO arena_report (id, donation_id, reporter_id, reason, status) VALUES (?, ?, ?, ?, ?)
        ON CONFLICT (donation_id, reporter_id) DO NOTHING
        """.trimIndent(),
        report.id, report.donationId, report.reporterId, report.reason, report.status.name,
    ) == 1

    fun findReport(id: UUID): ArenaReport? =
        jdbc.query("SELECT * FROM arena_report WHERE id = ?", REPORT, id).firstOrNull()

    fun openReports(): List<ArenaReport> =
        jdbc.query("SELECT * FROM arena_report WHERE status = 'OPEN' ORDER BY created_at", REPORT)

    /** 한 과녁의 열린 신고 전부를 같은 결정으로 닫는다. */
    fun resolveReports(donationId: UUID, status: ReportStatus, reviewer: String, resolution: String): Int = jdbc.update(
        """
        UPDATE arena_report SET status = ?, resolved_by = ?, resolved_at = now(), resolution = ?
         WHERE donation_id = ? AND status = 'OPEN'
        """.trimIndent(),
        status.name, reviewer, resolution, donationId,
    )

    fun export(userId: String): Map<String, Any?> = mapOf(
        "donations" to jdbc.query(
            "SELECT id, problem_id, submission_id, note, status, kind, reason, created_at FROM arena_donation WHERE donor_user_id = ? ORDER BY created_at",
            { rs, _ ->
                mapOf(
                    "id" to rs.getString(1), "problemId" to rs.getString(2), "submissionId" to rs.getString(3),
                    "note" to rs.getString(4), "status" to rs.getString(5), "kind" to rs.getString(6),
                    "reason" to rs.getString(7), "createdAt" to rs.getTimestamp(8).toInstant(),
                )
            },
            userId,
        ),
        "reports" to jdbc.query(
            "SELECT id, donation_id, reason, status, created_at FROM arena_report WHERE reporter_id = ? ORDER BY created_at",
            { rs, _ ->
                mapOf(
                    "id" to rs.getString(1), "donationId" to rs.getString(2), "reason" to rs.getString(3),
                    "status" to rs.getString(4), "createdAt" to rs.getTimestamp(5).toInstant(),
                )
            },
            userId,
        ),
    )

    /**
     * 삭제 (§11.3). 기부한 코드는 그 사람의 것이라 소스를 비운다 — 세워져 있던 과녁은
     * 그 순간 내려간다 (approved 는 source 가 있는 것만 센다). 기록판의 "누가"는 익명이 된다.
     */
    fun erase(userId: String): Int {
        jdbc.update("UPDATE arena_report SET reporter_id = 'erased:' || id::text WHERE reporter_id = ?", userId)
        return jdbc.update(
            "UPDATE arena_donation SET source = NULL, note = '', donor_user_id = 'erased:' || id::text WHERE donor_user_id = ?",
            userId,
        )
    }

    private companion object {
        val DONATION = RowMapper { rs, _ ->
            ArenaDonation(
                id = rs.getObject("id", UUID::class.java),
                problemId = rs.getString("problem_id"),
                submissionId = rs.getObject("submission_id", UUID::class.java),
                donorUserId = rs.getString("donor_user_id"),
                source = rs.getString("source"),
                note = rs.getString("note"),
                status = DonationStatus.valueOf(rs.getString("status")),
                kind = rs.getString("kind")?.let { DefectKind.valueOf(it) },
                reviewedBy = rs.getString("reviewed_by"),
                reviewedAt = rs.getTimestamp("reviewed_at")?.toInstant(),
                reason = rs.getString("reason"),
                createdAt = rs.getTimestamp("created_at").toInstant(),
            )
        }
        val REPORT = RowMapper { rs, _ ->
            ArenaReport(
                id = rs.getObject("id", UUID::class.java),
                donationId = rs.getObject("donation_id", UUID::class.java),
                reporterId = rs.getString("reporter_id"),
                reason = rs.getString("reason"),
                status = ReportStatus.valueOf(rs.getString("status")),
                resolvedBy = rs.getString("resolved_by"),
                resolvedAt = rs.getTimestamp("resolved_at")?.toInstant(),
                resolution = rs.getString("resolution"),
                createdAt = rs.getTimestamp("created_at").toInstant(),
            )
        }
    }
}
