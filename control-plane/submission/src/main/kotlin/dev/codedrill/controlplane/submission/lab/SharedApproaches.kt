package dev.codedrill.controlplane.submission.lab

/**
 * 실험실에 세울 수 있는 남의 풀이 (§3.1 조립 지점, 기획서 §8.5 "실행 가능한 인터랙티브 해설").
 *
 * 공유된 풀이는 게시판의 것이다. 실험실은 그것이 어디서 왔는지 모르고, 이름·언어·소스만
 * 받아 참조 풀이와 나란히 돌린다. 저작자의 해설이 "왜 맞나"라면, 남의 풀이를 같은 입력에
 * 세우는 것은 "다른 접근은 어디가 다른가"다 — 정적 풀이가 실행 가능한 해설이 되는 자리다.
 *
 * 맞힌 사람에게만 나간다. 부르는 쪽이 그것을 확인한다 — 해설을 미리 연 사람은 참조 풀이는
 * 봤지만 남의 코드까지 본 것은 아니다.
 */
fun interface SharedApproaches {

    fun forProblem(problemId: String): List<SharedApproach>

    companion object {
        val NONE = SharedApproaches { emptyList() }
    }
}

data class SharedApproach(val label: String, val language: String, val source: String)
