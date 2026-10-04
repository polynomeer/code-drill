package dev.codedrill.controlplane.analytics

/**
 * Analytics 모듈 경계 (디자인 설계서 §16.1 UX 이벤트, §16.2 계측 원칙).
 *
 * - 소유 데이터: UX 이벤트(ux_event). 누가 했는지가 아니라 **무엇이 쓰였나**를 센다 — 계정 id 를 받지 않는다.
 * - 제공 인터페이스: 이벤트 받기
 * - 금지 의존성: 다른 도메인 모듈. 판정·제출의 사실은 각자의 표에 있고 여기서 다시 세지 않는다.
 */
internal object AnalyticsModule
