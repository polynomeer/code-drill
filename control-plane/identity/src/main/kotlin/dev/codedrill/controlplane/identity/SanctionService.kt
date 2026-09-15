package dev.codedrill.controlplane.identity

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * 제재와 이의 (§8.5, §10.4).
 *
 * 발부는 보안 관리자가 근거를 들어 한다. 막는 것은 문 앞 — [AuthInterceptor] — 이고 이
 * 서비스는 "지금 무엇이 막히나"에 답한다. 이의는 제재 하나에 한 번이고, **발부한 사람은
 * 판단하지 못한다** — 재채점 승인과 같은 2인 원칙이다.
 */
@Service
class SanctionService(private val repository: SanctionRepository) {

    /** 지금 효력이 있는 제재. 없으면 null. 문 앞에서 매 요청마다 묻는다. */
    fun active(userId: String): Sanction? = repository.activeOf(userId, Instant.now())

    /**
     * 사용자에게 보이는 것: 효력 있는 제재가 먼저, 없으면 이의 중인 것, 없으면 가장 최근 것.
     * 풀린 것도 보인다 — 이의의 답이 거기 실리고, 그것을 보는 것이 이의 절차의 끝이다 (§10.4).
     */
    fun mine(userId: String): SanctionView? {
        val all = repository.of(userId)
        val chosen = all.firstOrNull { it.active() } ?: all.firstOrNull { it.appealPending } ?: all.firstOrNull()
        return chosen?.let { SanctionView.of(it) }
    }

    fun history(userId: String): List<Sanction> = repository.of(userId)

    @Transactional
    fun issue(userId: String, kind: SanctionKind, reason: String, evidence: String, days: Int?, issuedBy: String): Outcome {
        val cleanReason = reason.trim()
        if (cleanReason.length < MIN_REASON) return Outcome.Rejected("사유를 ${MIN_REASON}자 이상 적는다")
        if (!EVIDENCE.matches(evidence.trim())) return Outcome.Rejected("근거는 similarity:<id> 나 report:<id> 꼴이어야 한다")
        if (userId == issuedBy) return Outcome.Rejected("자기 자신에게는 제재를 걸 수 없다")
        val ends = when (kind) {
            SanctionKind.WARNING -> null
            else -> {
                val d = days ?: return Outcome.Rejected("정지에는 기간(일)이 필요하다")
                if (d !in 1..MAX_DAYS) return Outcome.Rejected("기간은 1~${MAX_DAYS}일")
                Instant.now().plus(Duration.ofDays(d.toLong()))
            }
        }
        val sanction = Sanction(
            id = UUID.randomUUID(), userId = userId, kind = kind, reason = cleanReason, evidence = evidence.trim(),
            issuedBy = issuedBy, startsAt = Instant.now(), endsAt = ends, liftedBy = null, liftedAt = null,
            appeal = null, appealedAt = null, appealResolution = null, appealNote = null, resolvedBy = null, resolvedAt = null,
            createdAt = Instant.now(),
        )
        repository.insert(sanction)
        return Outcome.Decided(sanction)
    }

    @Transactional
    fun lift(id: UUID, by: String): Outcome {
        val sanction = repository.find(id) ?: return Outcome.Rejected("그런 제재가 없다")
        if (repository.lift(id, by) == 0) return Outcome.Rejected("이미 풀린 제재다")
        return Outcome.Decided(repository.find(id) ?: sanction)
    }

    /** 이의 (§10.4). 제재 하나에 한 번. 효력이 끝난 제재에도 낼 수 있다 — 기록을 바로잡는 것도 이의다. */
    @Transactional
    fun appeal(userId: String, id: UUID, text: String): AppealOutcome {
        val clean = text.trim()
        if (clean.length < MIN_APPEAL) return AppealOutcome.Invalid("이의는 ${MIN_APPEAL}자 이상 적는다")
        val sanction = repository.find(id)?.takeIf { it.userId == userId } ?: return AppealOutcome.Invalid("그런 제재가 없다")
        if (sanction.appeal != null) return AppealOutcome.AlreadyAppealed
        repository.appeal(id, userId, clean)
        return AppealOutcome.Filed(SanctionView.of(repository.find(id)!!))
    }

    fun openAppeals(): List<Sanction> = repository.openAppeals()

    /** 이의를 판단한다. 발부한 사람은 못 한다. 받아들이면 제재를 푼다. */
    @Transactional
    fun resolveAppeal(id: UUID, by: String, uphold: Boolean, note: String?): Outcome {
        val sanction = repository.find(id) ?: return Outcome.Rejected("그런 제재가 없다")
        if (!sanction.appealPending) return Outcome.Rejected("열린 이의가 없다")
        if (sanction.issuedBy == by) return Outcome.Rejected("발부한 사람은 이의를 판단하지 못한다 — 다른 보안 관리자가 본다")
        val resolution = if (uphold) AppealResolution.UPHELD else AppealResolution.LIFTED
        repository.resolveAppeal(id, resolution, by, note?.trim()?.ifBlank { null })
        if (!uphold) repository.lift(id, by)
        return Outcome.Decided(repository.find(id)!!)
    }

    sealed interface Outcome {
        data class Decided(val sanction: Sanction) : Outcome
        data class Rejected(val reason: String) : Outcome
    }

    sealed interface AppealOutcome {
        data class Filed(val view: SanctionView) : AppealOutcome
        data object AlreadyAppealed : AppealOutcome
        data class Invalid(val reason: String) : AppealOutcome
    }

    companion object {
        const val MIN_REASON = 10
        const val MIN_APPEAL = 20
        const val MAX_DAYS = 365
        private val EVIDENCE = Regex("^(similarity|report):[0-9a-fA-F-]{36}$")
    }
}
