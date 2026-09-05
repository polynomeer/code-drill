package dev.codedrill.controlplane.problem

/**
 * Problem 모듈 경계 (기술 설계서 §3.1).
 *
 * - 소유 데이터: problem, version, competency tag, pack
 * - 제공 인터페이스: 공개 문제·버전·학습 자산 조회
 * - 금지 의존성: Submission 테이블 직접 수정
 *
 * 다른 도메인 모듈을 직접 참조하지 않는다. 협력은 조립 지점인
 * `:control-plane:app` 에서 연결하며, 경계 위반은 `gradle checkModuleBoundaries` 가 잡는다.
 */
internal object ProblemModule
