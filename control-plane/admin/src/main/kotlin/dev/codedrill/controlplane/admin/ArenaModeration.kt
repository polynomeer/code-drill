package dev.codedrill.controlplane.admin

import java.util.UUID

/**
 * Admin 이 아레나에 요청하는 것 (기술 설계서 §3.1 모듈 경계, 기획서 §8.5 검수).
 *
 * Admin 은 기부·신고 표를 직접 읽지 않는다. 검수는 "무엇을 세울지 결정"하는 일이고,
 * 세우고 내리는 것은 아레나의 것이다. 여기에는 검수자가 봐야 할 것과 내릴 수 있는
 * 결정만 있다.
 */
interface ArenaModeration {

    /** 검수를 기다리는 기부와 열린 신고. 소스가 실린다 — 검수자는 코드를 봐야 한다. */
    fun queue(): Any

    /** 세운다. [kind] 는 DefectKind 의 이름이다. 결정이 되면 결과를, 안 되면 null 과 사유를. */
    fun approve(id: UUID, reviewer: String, kind: String, note: String?): Decision

    fun reject(id: UUID, reviewer: String, reason: String): Decision

    /** 신고를 처리한다. [retire] 면 과녁을 내리고, 아니면 신고를 기각한다. */
    fun resolve(reportId: UUID, reviewer: String, retire: Boolean, resolution: String): Decision

    data class Decision(val result: Any?, val rejected: String?) {
        companion object {
            fun ok(result: Any) = Decision(result, null)
            fun rejected(reason: String) = Decision(null, reason)
        }
    }
}
