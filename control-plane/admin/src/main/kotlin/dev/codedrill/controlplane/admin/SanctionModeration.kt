package dev.codedrill.controlplane.admin

import java.util.UUID

/**
 * Admin 이 계정 제재에 요청하는 것 (§3.1 모듈 경계, §8.5 단계적 제재, §10.4 이의 절차).
 *
 * 제재는 계정의 상태라 Identity 의 것이고, Admin 은 결정만 넘긴다. 발부·해제·이의 판단
 * 전부 SECURITY_ADMIN 이다 — 계정에 닿는 결정은 콘텐츠 검수와 다른 역할이 본다.
 */
interface SanctionModeration {

    /** [kind] 는 WARNING / MUTE / SUSPEND. [evidence] 는 similarity:<id> 나 report:<id>. */
    fun issue(userId: String, kind: String, reason: String, evidence: String, days: Int?, issuedBy: String): ArenaModeration.Decision

    fun lift(id: UUID, by: String): ArenaModeration.Decision

    /** 열린 이의. */
    fun appeals(): Any

    /** 이의를 판단한다. 발부한 사람은 못 한다. */
    fun resolveAppeal(id: UUID, by: String, uphold: Boolean, note: String?): ArenaModeration.Decision

    /** 한 사람의 제재 이력. */
    fun history(userId: String): Any
}
