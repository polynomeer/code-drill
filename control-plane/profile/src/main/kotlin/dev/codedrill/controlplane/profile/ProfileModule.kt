package dev.codedrill.controlplane.profile

/**
 * Profile 모듈 경계 (docs/ui-overhaul.md §6.7).
 *
 * - 소유 데이터: 없다. 공개 프로필은 다른 도메인의 사실을 모아 **누구에게 무엇을 보이는가**를 정하는
 *   읽기 모델이다 — 그 규칙이 이 모듈의 몫이다.
 * - 제공 인터페이스: 핸들로 공개 프로필 읽기
 * - 금지 의존성: 다른 도메인 모듈 직접 참조. 사실은 조립 지점이 [ProfileSources] 로 잇는다.
 */
internal object ProfileModule
