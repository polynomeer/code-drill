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
interface LearningSignals {

    /** 판정이 확정됐다. 재채점으로 바뀐 판정은 부르지 않는다. */
    fun judged(userId: String, problemId: String, submissionId: UUID, accepted: Boolean)

    /**
     * 리플레이에서 다음 이벤트를 맞혀 봤다 (FR-805).
     *
     * 재는 것은 판정이 아니라 **자기 코드가 무엇을 하는지 아는가**다. 어느 역량의
     * 증거인지는 Competency 가 정한다.
     */
    fun predicted(userId: String, problemId: String, predictionId: String, correct: Boolean)

    companion object {
        val NONE = object : LearningSignals {
            override fun judged(
                userId: String,
                problemId: String,
                submissionId: UUID,
                accepted: Boolean,
            ) = Unit

            override fun predicted(
                userId: String,
                problemId: String,
                predictionId: String,
                correct: Boolean,
            ) = Unit
        }
    }
}
