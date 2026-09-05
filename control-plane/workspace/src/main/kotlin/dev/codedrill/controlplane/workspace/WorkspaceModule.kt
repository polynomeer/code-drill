package dev.codedrill.controlplane.workspace

/**
 * Workspace 모듈 경계 (기술 설계서 §3.1).
 *
 * - 소유 데이터: draft, custom test, response, preference
 * - 제공 인터페이스: 자동 저장·질문 응답·사용자 테스트
 * - 금지 의존성: 판정 상태 변경
 *
 * 다른 도메인 모듈을 직접 참조하지 않는다. 협력은 조립 지점인
 * `:control-plane:app` 에서 연결하며, 경계 위반은 `gradle checkModuleBoundaries` 가 잡는다.
 */
internal object WorkspaceModule
