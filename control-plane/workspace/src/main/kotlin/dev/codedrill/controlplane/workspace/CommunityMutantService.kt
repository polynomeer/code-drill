package dev.codedrill.controlplane.workspace

import dev.codedrill.platform.problempackage.DefectKind
import dev.codedrill.platform.problempackage.MutantSource
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

/**
 * 남의 오답을 아레나에 세우는 절차 (기획서 §8.3, §8.5).
 *
 * 사용자 쪽(기부·신고)과 검수자 쪽(승인·반려·내림)이 한 서비스에 있다. 둘이 같은 표의
 * 같은 상태 기계를 움직이므로, 갈라 두면 상태 전이가 두 곳에 산다.
 */
@Service
class CommunityMutantService(
    private val repository: CommunityMutantRepository,
    private val submissions: DonatableSubmissions = DonatableSubmissions.NONE,
    private val gate: ArenaGate = ArenaGate.CLOSED,
) {

    // --- 사용자 -----------------------------------------------------------------

    /** 이 문제에서 내놓을 수 있는 내 오답. 이미 내놓은 것은 뺀다. */
    fun donatable(userId: String, problemId: String): List<DonatableSubmission> =
        submissions.donatable(userId, problemId).filter { repository.findBySubmission(it.id) == null }

    fun mine(userId: String, problemId: String): List<ArenaDonation> = repository.mine(userId, problemId)

    /**
     * 내 오답을 내놓는다. **틀린 제출만, 본인만.** 맞힌 문제가 아니어도 된다 — 내놓는 것과
     * 깨뜨리는 것은 다른 일이고, 틀린 사람이야말로 내놓을 것이 있다.
     */
    @Transactional
    fun donate(userId: String, submissionId: UUID, note: String): DonateOutcome {
        val trimmed = note.trim()
        if (trimmed.length < MIN_NOTE) return DonateOutcome.Invalid("무엇을 잘못하는지 ${MIN_NOTE}자 이상 적는다")
        val submission = submissions.donatable(userId, submissionId)
            ?: return DonateOutcome.Invalid("내 것이고, 판정이 오답이고, Kotlin 인 제출만 내놓을 수 있다")
        repository.findBySubmission(submissionId)?.let { return DonateOutcome.Duplicate(it) }

        val donation = ArenaDonation(
            id = UUID.randomUUID(),
            problemId = submission.problemId,
            submissionId = submissionId,
            donorUserId = userId,
            source = submission.source,
            note = trimmed,
            status = DonationStatus.PENDING,
            kind = null, reviewedBy = null, reviewedAt = null, reason = null,
            createdAt = Instant.now(),
        )
        repository.insert(donation)
        return DonateOutcome.Accepted(donation)
    }

    /**
     * 세워진 오답을 신고한다. **맞힌 사람만** — 과녁을 본 사람만 신고할 수 있다.
     * 두 번째 신고는 조용히 무시된다. 신고는 검수 큐에 오르고, 내리는 것은 검수자다.
     */
    @Transactional
    fun report(userId: String, problemId: String, targetName: String, reason: String): ReportOutcome {
        if (!gate.solved(userId, problemId)) return ReportOutcome.Locked
        val trimmed = reason.trim()
        if (trimmed.length < MIN_NOTE) return ReportOutcome.Invalid("사유를 ${MIN_NOTE}자 이상 적는다")
        val donation = repository.approved(problemId).firstOrNull { it.targetName == targetName }
            ?: return ReportOutcome.Invalid("그 이름의 세워진 오답이 없다")
        val report = ArenaReport(
            id = UUID.randomUUID(), donationId = donation.id, reporterId = userId, reason = trimmed,
            status = ReportStatus.OPEN, resolvedBy = null, resolvedAt = null, resolution = null, createdAt = Instant.now(),
        )
        return if (repository.insertReport(report)) ReportOutcome.Filed(report) else ReportOutcome.AlreadyFiled
    }

    /** 아레나가 과녁으로 쓰는 것. 세워졌고 소스가 남아 있는 것만. */
    fun targets(problemId: String): List<MutantSource> = repository.approved(problemId).map { it.asMutant() }

    // --- 검수자 -----------------------------------------------------------------

    fun queue(): ReviewQueue = ReviewQueue(
        pending = repository.pending(),
        reports = repository.openReports().map { report ->
            ReportedTarget(report, repository.find(report.donationId))
        },
    )

    /** 세운다. 결함군은 검수자가 정한다 — 기부자는 자기 오답이 무슨 종류인지 모르는 것이 보통이다. */
    @Transactional
    fun approve(id: UUID, reviewer: String, kind: DefectKind, note: String?): ReviewOutcome {
        if (!kind.reachableByHandWrittenCase) return ReviewOutcome.Rejected("손으로 적는 입력으로 깨뜨릴 수 있는 종류여야 한다")
        val donation = repository.find(id) ?: return ReviewOutcome.Rejected("그런 기부가 없다")
        if (donation.source == null) return ReviewOutcome.Rejected("기부자가 계정을 지워 소스가 없다")
        if (repository.review(id, DonationStatus.APPROVED, kind, reviewer, note?.trim()?.ifBlank { null }) == 0) {
            return ReviewOutcome.Rejected("이미 결정된 기부다: ${donation.status}")
        }
        return ReviewOutcome.Decided(repository.find(id)!!)
    }

    @Transactional
    fun reject(id: UUID, reviewer: String, reason: String): ReviewOutcome {
        val donation = repository.find(id) ?: return ReviewOutcome.Rejected("그런 기부가 없다")
        if (repository.review(id, DonationStatus.REJECTED, null, reviewer, reason.trim()) == 0) {
            return ReviewOutcome.Rejected("이미 결정된 기부다: ${donation.status}")
        }
        return ReviewOutcome.Decided(repository.find(id)!!)
    }

    /**
     * 신고를 처리한다. 내리면 그 과녁의 열린 신고 전부가 함께 닫힌다 — 같은 과녁을 두고
     * 하나는 내리고 하나는 기각하는 결정은 없다.
     */
    @Transactional
    fun resolve(reportId: UUID, reviewer: String, retire: Boolean, resolution: String): ReviewOutcome {
        val report = repository.findReport(reportId) ?: return ReviewOutcome.Rejected("그런 신고가 없다")
        if (report.status != ReportStatus.OPEN) return ReviewOutcome.Rejected("이미 처리된 신고다: ${report.status}")
        val donation = repository.find(report.donationId) ?: return ReviewOutcome.Rejected("신고된 기부가 없다")
        val trimmed = resolution.trim()
        if (retire) {
            repository.retire(donation.id, reviewer, trimmed)
            repository.resolveReports(donation.id, ReportStatus.RETIRED, reviewer, trimmed)
        } else {
            repository.resolveReports(donation.id, ReportStatus.DISMISSED, reviewer, trimmed)
        }
        return ReviewOutcome.Decided(repository.find(donation.id)!!)
    }

    private fun ArenaDonation.asMutant() = MutantSource(
        name = targetName,
        kind = kind ?: DefectKind.UNSPECIFIED,
        source = source.orEmpty(),
        // 검수자가 다듬은 설명이 있으면 그것, 없으면 기부자의 말.
        note = reason?.takeIf { status == DonationStatus.APPROVED && it.isNotBlank() } ?: note,
    )

    sealed interface DonateOutcome {
        data class Accepted(val donation: ArenaDonation) : DonateOutcome
        data class Duplicate(val donation: ArenaDonation) : DonateOutcome
        data class Invalid(val reason: String) : DonateOutcome
    }

    sealed interface ReportOutcome {
        data class Filed(val report: ArenaReport) : ReportOutcome
        data object AlreadyFiled : ReportOutcome
        data object Locked : ReportOutcome
        data class Invalid(val reason: String) : ReportOutcome
    }

    sealed interface ReviewOutcome {
        data class Decided(val donation: ArenaDonation) : ReviewOutcome
        data class Rejected(val reason: String) : ReviewOutcome
    }

    data class ReviewQueue(val pending: List<ArenaDonation>, val reports: List<ReportedTarget>)
    data class ReportedTarget(val report: ArenaReport, val donation: ArenaDonation?)

    private companion object {
        const val MIN_NOTE = 10
    }
}
