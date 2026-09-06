package dev.codedrill.controlplane.problem

/**
 * 공개된 문제가 무엇인지 알려 주는 창구 (기술 설계서 §3.1, §3.2).
 *
 * Problem 모듈은 Admin 모듈을 참조하지 않는다. 공개 여부는 Admin 이 소유하는 상태이므로,
 * 여기서는 필요한 질문만 인터페이스로 두고 조립 지점인 `:control-plane:app` 이 연결한다.
 *
 * 이 경계가 없으면 "목록을 고치려다 공개 정책을 함께 고치는" 변경이 쉬워진다.
 */
fun interface PublishedProblems {
    fun ids(): Set<String>
}
