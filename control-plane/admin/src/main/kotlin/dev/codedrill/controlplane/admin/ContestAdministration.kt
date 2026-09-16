package dev.codedrill.controlplane.admin

import java.time.Instant
import java.util.UUID

/**
 * Admin 이 대회에 요청하는 것 (§3.1 모듈 경계, §8.4).
 *
 * 대회를 만들고 공개하는 것은 콘텐츠를 공개하는 것과 같은 성격이라 CONTENT_EDITOR 가
 * 만들고 PUBLISHER 가 연다 — 문제 공개의 2인 원칙을 그대로 쓴다.
 */
interface ContestAdministration {

    /** [kind] 는 CONTEST 나 HACK. 대결은 사용자가 연다. */
    fun create(createdBy: String, kind: String, title: String, problemIds: List<String>, startsAt: Instant, endsAt: Instant): ArenaModeration.Decision

    /** 만든 사람은 못 연다 — 2인 원칙. */
    fun publish(id: UUID, actor: String): ArenaModeration.Decision
}
