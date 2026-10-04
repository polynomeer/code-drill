package dev.codedrill.controlplane.notification

/**
 * Notification 모듈 경계 (docs/ui-overhaul.md §4 전역 헤더 "알림").
 *
 * - 소유 데이터: 어디까지 읽었나(read_until) 하나. 알림 자체는 저장하지 않는다 — 다른 도메인에 이미 있는
 *   사실(대회 시작, 레이팅 변화, 답, 도움됐다, 전이 확인, 제재)에서 그때그때 만든다.
 * - 제공 인터페이스: 내 알림 목록과 안 읽은 수, 읽음 표시
 * - 금지 의존성: 다른 도메인 모듈 직접 참조. 사실은 조립 지점이 [NotificationSources] 로 잇는다.
 */
internal object NotificationModule
