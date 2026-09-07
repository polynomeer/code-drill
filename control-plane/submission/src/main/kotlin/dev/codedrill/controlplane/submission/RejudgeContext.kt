package dev.codedrill.controlplane.submission

import java.util.UUID

/**
 * 이 판정이 재채점의 결과인지, 그렇다면 어떻게 다뤄야 하는지 (기술 설계서 §3.1 조립 지점).
 *
 * 제출 모듈은 재채점을 모른다. 재채점은 Admin 의 관심사이고, 둘을 잇는 결정은
 * :control-plane:app 한 곳에 모인다. 여기가 그 창구다.
 *
 * 기본 구현은 [NONE] 이다. Admin 이 붙지 않은 조립에서도 최초 판정은 정상적으로
 * 반영돼야 하므로, "재채점이 아니다"가 안전한 기본값이다.
 */
interface RejudgeContext {

    /** 이 제출이 기다리고 있는 재채점. null 이면 사용자가 직접 제출한 판정이다. */
    fun pendingFor(submissionId: UUID): Pending?

    /** 판정을 기록했다. 대상 표시와 감사 기록은 Admin 이 맡는다 (§13.3). */
    fun judged(outcome: Outcome)

    data class Pending(
        val jobId: UUID,
        /** true 면 이력에만 남기고 현재 판정은 그대로 둔다. */
        val dryRun: Boolean,
    )

    /**
     * 재채점이 무엇을 무엇으로 바꿨는지.
     *
     * 바뀌지 않은 것도 함께 보낸다. "재채점했는데 아무것도 안 바뀌었다"는 결과 자체가
     * 답이며, 감사에서 가장 자주 필요한 사실이다.
     */
    data class Outcome(
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

    companion object {
        /** 재채점이 없는 조립. 모든 판정은 최초 판정으로 다뤄진다. */
        val NONE = object : RejudgeContext {
            override fun pendingFor(submissionId: UUID): Pending? = null
            override fun judged(outcome: Outcome) = Unit
        }
    }
}
