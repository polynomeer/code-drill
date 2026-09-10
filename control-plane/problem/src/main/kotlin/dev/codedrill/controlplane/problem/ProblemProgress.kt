package dev.codedrill.controlplane.problem

/**
 * 문제별 성과를 알려 주는 창구 (기술 설계서 §3.1, PRD FR-201·FR-203).
 *
 * Problem 모듈은 Submission 테이블을 읽지 않는다. 그런데 목록에는 **정답률**과 **내가 푼
 * 문제**가 보여야 하고, 그 둘은 제출 도메인이 소유하는 사실이다. 그래서 질문 둘만 열고
 * 조립 지점인 `:control-plane:app` 이 연결한다.
 *
 * 이 경계가 없으면 목록 쿼리 하나 때문에 Problem 이 제출 스키마를 알게 되고, 제출 테이블을
 * 고칠 때마다 문제 목록이 함께 깨진다.
 */
interface ProblemProgress {

    /**
     * 문제별 시도·정답 사용자 수.
     *
     * **사람 수를 센다. 제출 수가 아니다.** 제출 수로 세면 한 사람이 열 번 틀린 문제의
     * 정답률이 실제보다 훨씬 낮게 나오고, 그 값은 문제의 난이도가 아니라 그 사람의
     * 끈기를 말한다 (§10.2 — 내부 난이도와 사용자 성과를 분리해 관리한다).
     */
    fun accuracy(): Map<String, Accuracy>

    /** 이 사용자가 한 번이라도 맞힌 문제. 로그인하지 않았으면 부르지 않는다. */
    fun solvedBy(userId: String): Set<String>

    data class Accuracy(val attempted: Int, val solved: Int) {
        /**
         * 표본이 적으면 비율을 내지 않는다.
         *
         * 한 사람이 풀어 100%, 한 사람이 틀려 0% 인 문제가 목록에서 가장 쉽고 가장 어려운
         * 문제로 나란히 보이면, 그 숫자는 정보가 아니라 잡음이다.
         */
        val rate: Double? get() = if (attempted >= MINIMUM_SAMPLE) solved.toDouble() / attempted else null

        private companion object {
            const val MINIMUM_SAMPLE = 5
        }
    }
}
