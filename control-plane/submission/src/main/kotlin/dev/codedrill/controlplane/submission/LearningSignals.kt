package dev.codedrill.controlplane.submission

import java.util.UUID

/**
 * 판정이 학습 기록에 남는 창구 (기술 설계서 §3.1 조립 지점).
 *
 * 제출 모듈은 역량을 모른다. 어느 판정이 어느 역량의 증거가 되는지는 Competency 의
 * 관심사이고, 둘을 잇는 결정은 `:control-plane:app` 한 곳에 모인다.
 *
 * 기본 구현은 [NONE] 이다. Competency 가 붙지 않은 조립에서도 채점은 그대로 돌아야 한다 —
 * **학습 기록이 판정을 막아서는 안 된다.**
 */
fun interface LearningSignals {

    /** 판정이 확정됐다. 재채점으로 바뀐 판정은 부르지 않는다. */
    fun judged(userId: String, problemId: String, submissionId: UUID, accepted: Boolean)

    companion object {
        val NONE = LearningSignals { _, _, _, _ -> }
    }
}
