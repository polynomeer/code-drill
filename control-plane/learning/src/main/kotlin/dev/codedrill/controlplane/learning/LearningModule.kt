package dev.codedrill.controlplane.learning

/**
 * Learning 모듈 경계 (기술 설계서 §3.1).
 *
 * - 소유 데이터: problem state, collection, activity
 * - 제공 인터페이스: 문제집·진도·통계
 * - 금지 의존성: 채점 원본 로그 의존
 *
 * 다른 도메인 모듈을 직접 참조하지 않는다. 협력은 조립 지점인
 * `:control-plane:app` 에서 연결하며, 경계 위반은 `gradle checkModuleBoundaries` 가 잡는다.
 */
internal object LearningModule
