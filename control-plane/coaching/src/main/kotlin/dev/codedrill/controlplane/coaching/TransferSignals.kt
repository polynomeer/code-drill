package dev.codedrill.controlplane.coaching

import dev.codedrill.platform.problempackage.Competency

/**
 * 전이가 확인됐다는 사실이 학습 기록에 남는 창구 (§3.1 조립 지점, FR-807).
 *
 * Coaching 은 가중치를 정하지 않는다. "가장 높은 가중치"가 얼마인지는 다른 증거들과
 * 나란히 놓고 정할 일이고, 그것을 아는 곳은 Competency 다.
 */
fun interface TransferSignals {

    /** 힌트 없이 변형 문제를 통과했다. */
    fun transferred(
        userId: String,
        problemId: String,
        taskId: String,
        competencies: List<Competency>,
    )

    companion object {
        val NONE = TransferSignals { _, _, _, _ -> }
    }
}
