package dev.codedrill.controlplane.admin

import java.util.UUID

/**
 * Admin 이 유사도 신호에 요청하는 것 (§3.1 모듈 경계, §11.4 부정행위 방어).
 *
 * Admin 은 지문·신호 표를 직접 읽지 않는다. 검수는 "이 쌍이 베낀 것인가"를 사람이 두
 * 소스를 보고 결정하는 일이고, 결정은 기록으로만 남는다 — 판정도 계정도 건드리지 않는다
 * (§10.4 "탐지 신호를 단독 유죄 근거로 사용하지 않는다").
 */
interface IntegrityModeration {

    /** 열린 신호와 두 소스. 점수가 높은 것부터. */
    fun queue(): Any

    fun resolve(id: UUID, reviewer: String, confirmed: Boolean, note: String?): ArenaModeration.Decision
}
