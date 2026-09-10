package dev.codedrill.controlplane.coaching

import dev.codedrill.platform.problempackage.Competency

/**
 * 무엇이 약한지 물어보는 창구 (기술 설계서 §3.1 조립 지점).
 *
 * Coaching 은 숙련도를 계산하지 않는다. 그것은 Competency 의 일이고, 둘을 잇는 결정은
 * `:control-plane:app` 한 곳에 모인다.
 *
 * 기본 구현은 [NONE] 이다 — Competency 가 붙지 않은 조립에서도 코칭을 열 수는 있어야
 * 한다. 다만 그때는 개입할 역량이 정해지지 않으므로, 세션이 "도울 것이 없다"로 열린다.
 */
fun interface Diagnosis {

    /**
     * [among] 중 약한 순으로 최대 [limit] 개.
     *
     * **STRONG 은 돌려주지 않는다.** 잘하는 것에 힌트를 붙이면 그것은 개입이 아니라 방해다.
     * 그래서 결과가 비어 있을 수 있고, 그 경우가 정상이다.
     */
    fun weakest(userId: String, among: List<Competency>, limit: Int): List<Competency>

    companion object {
        val NONE = Diagnosis { _, _, _ -> emptyList() }
    }
}
