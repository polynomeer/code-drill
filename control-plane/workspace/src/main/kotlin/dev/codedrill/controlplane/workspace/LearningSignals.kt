package dev.codedrill.controlplane.workspace

import dev.codedrill.platform.problempackage.Competency

/**
 * 작업 공간에서 일어난 일이 학습 기록에 남는 창구 (기술 설계서 §3.1 조립 지점).
 *
 * Workspace 는 숙련도를 모른다. 사전 질문 응답과 시험 실행이 어느 역량의 증거가 되는지는
 * Competency 의 관심사이고, 둘을 잇는 결정은 `:control-plane:app` 한 곳에 모인다.
 *
 * 기본 구현은 [NONE] 이다. Competency 가 붙지 않은 조립에서도 질문과 시험 실행은 그대로
 * 돌아야 한다 — **학습 기록이 기능을 막아서는 안 된다.**
 */
interface LearningSignals {

    /** 풀이 전 질문에 답했다 (FR-803). */
    fun answered(
        userId: String,
        problemId: String,
        competency: Competency,
        correct: Boolean,
        reference: String,
        detail: String?,
    )

    /**
     * 시험 실행이 끝났다.
     *
     * [judgedCases] 는 **기대 출력을 적은** 케이스 수다. 입력만 넣고 출력을 구경한 것은
     * 시험이 아니라 실행이므로, 그 둘을 세는 쪽에서 갈라 준다.
     */
    fun tested(userId: String, problemId: String, trialId: String, judgedCases: Int)

    companion object {
        val NONE = object : LearningSignals {
            override fun answered(
                userId: String,
                problemId: String,
                competency: Competency,
                correct: Boolean,
                reference: String,
                detail: String?,
            ) = Unit

            override fun tested(userId: String, problemId: String, trialId: String, judgedCases: Int) = Unit
        }
    }
}

/**
 * 질문 종류 → 역량 (기획서 §4.2).
 *
 * 이 대응을 Competency 쪽에 두지 않는다. 질문을 만드는 쪽이 그 질문이 무엇을 재는지 알고
 * 있으며, 저쪽에 두면 Competency 가 Workspace 의 타입을 알아야 한다.
 */
fun QuestionKind.competency(): Competency = when (this) {
    QuestionKind.ALGORITHM_CHOICE -> Competency.ALGORITHM_CHOICE
    QuestionKind.TIME_COMPLEXITY -> Competency.COMPLEXITY
    QuestionKind.SPACE_COMPLEXITY -> Competency.COMPLEXITY
}
