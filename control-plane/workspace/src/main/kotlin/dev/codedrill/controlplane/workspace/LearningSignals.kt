package dev.codedrill.controlplane.workspace

import dev.codedrill.platform.problempackage.Competency
import dev.codedrill.platform.problempackage.DefectKind

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

    /**
     * 변이 평가가 끝났다 (FR-804).
     *
     * **사실만 넘긴다.** 어느 결함군이 어느 역량의 증거인지, 몇 개를 잡아야 성공인지는
     * Competency 가 정한다 — 여기서 정하면 같은 판단이 두 모듈에 흩어진다.
     *
     * [killedByKind] 는 결함군별 (잡은 수, 전체 수)다. 합계만 넘기면 "경계 입력을 전부
     * 놓쳤다"와 "성능만 못 잡았다"가 같은 숫자가 되는데, 그 둘에게 필요한 다음 행동은
     * 다르다.
     */
    fun mutationChecked(
        userId: String,
        problemId: String,
        evaluationId: String,
        killedByKind: Map<DefectKind, Pair<Int, Int>>,
    )

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

            override fun mutationChecked(
                userId: String,
                problemId: String,
                evaluationId: String,
                killedByKind: Map<DefectKind, Pair<Int, Int>>,
            ) = Unit
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
