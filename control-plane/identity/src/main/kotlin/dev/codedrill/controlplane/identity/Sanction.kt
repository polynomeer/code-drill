package dev.codedrill.controlplane.identity

import java.time.Instant
import java.util.UUID

/**
 * 제재 (기획서 §8.5 "단계적 제재", §10.4 "이의 절차").
 *
 * 세 단계다. 경고는 기록이고 아무것도 막지 않는다. 글쓰기 정지는 커뮤니티의 쓰기 —
 * 질문·답·풀이·기부·신고 — 를 막는다. 제출 정지는 실행을 막고 글쓰기도 함께 막는다.
 * 영구 정지는 없다; 그것은 삭제이고 삭제는 본인만 한다 (§11.3).
 *
 * 제재는 [evidence] 를 가리킨다 — 유사도 신호나 신고의 id. 근거 없는 제재는 없다. 이의는
 * 제재 하나에 한 번이고, 발부한 사람이 아닌 보안 관리자가 판단한다.
 */
data class Sanction(
    val id: UUID,
    val userId: String,
    val kind: SanctionKind,
    val reason: String,
    val evidence: String,
    val issuedBy: String,
    val startsAt: Instant,
    /** 경고는 null. 정지는 끝이 있다. */
    val endsAt: Instant?,
    val liftedBy: String?,
    val liftedAt: Instant?,
    val appeal: String?,
    val appealedAt: Instant?,
    val appealResolution: AppealResolution?,
    val appealNote: String?,
    val resolvedBy: String?,
    val resolvedAt: Instant?,
    val createdAt: Instant,
) {
    /** 지금 효력이 있나. 경고는 막는 것이 없으므로 효력을 묻지 않는다. */
    fun active(now: Instant = Instant.now()): Boolean =
        kind != SanctionKind.WARNING && liftedAt == null && (endsAt == null || endsAt.isAfter(now)) && !startsAt.isAfter(now)

    /** 이의를 냈고 아직 답이 없다. */
    val appealPending: Boolean get() = appeal != null && appealResolution == null
}

enum class SanctionKind(val blocksWriting: Boolean, val blocksExecution: Boolean) {
    WARNING(false, false),
    MUTE(true, false),
    SUSPEND(true, true),
}

enum class AppealResolution { UPHELD, LIFTED }

/**
 * 사용자에게 보이는 모양. 발부자·근거 id 는 나가지 않는다 — 근거는 관리자의 것이고, 사용자가
 * 보는 것은 사유와 기간, 그리고 이의의 상태다.
 */
data class SanctionView(
    val id: UUID,
    val kind: SanctionKind,
    val reason: String,
    val startsAt: Instant,
    val endsAt: Instant?,
    /** 지금 막고 있나. 풀렸거나 끝났거나 경고면 false — 그래도 보이는 이유는 이의의 답이 여기 실리기 때문이다. */
    val active: Boolean,
    val liftedAt: Instant?,
    val appealed: Boolean,
    val appealResolution: AppealResolution?,
    val appealNote: String?,
) {
    companion object {
        fun of(s: Sanction) = SanctionView(
            s.id, s.kind, s.reason, s.startsAt, s.endsAt, s.active(), s.liftedAt, s.appeal != null, s.appealResolution, s.appealNote,
        )
    }
}
