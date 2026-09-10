package dev.codedrill.controlplane.coaching

import java.time.Instant
import java.util.UUID

/**
 * 전이 확인 과제 (PRD FR-807).
 *
 * > 코칭 후 설명 과제와 변형 문제로 전이를 확인합니다. 힌트·AI 없이 완료한 결과를 가장
 * > 높은 가중치의 증거로 반영합니다.
 *
 * **코칭받은 문제를 다시 푸는 것은 전이가 아니다.** 힌트를 보고 그 자리에서 고친 것은
 * 그 문제를 푼 것이고, 옮겨졌는지는 **다른 문제**에서만 드러난다.
 *
 * 설명이 먼저다. 자기 말로 정리하지 않고 다음 문제로 넘어가면, 맞혔을 때 그것이 옮겨진
 * 것인지 비슷한 모양을 기억한 것인지 가릴 수 없다.
 */
data class TransferTask(
    val id: UUID,
    val sessionId: UUID,
    val userId: String,
    /** 코칭받은 문제. */
    val sourceProblemId: String,
    /** 배움을 옮겨 볼 문제. */
    val targetProblemId: String,
    val explanation: String?,
    val status: TransferStatus,
    /** 변형 문제를 풀 때 받은 도움 단계. 끝나기 전에는 null 이다. */
    val helpLevel: Int?,
    val createdAt: Instant,
    /** 끝나기 전에는 null. 통과하지 못한 판정은 과제를 닫지 않는다. */
    val completedAt: Instant? = null,
)

enum class TransferStatus {
    /** 변형 문제를 골라 두었다. 아직 설명도 풀이도 없다. */
    ASSIGNED,

    /** 설명을 썼다. 이제 변형 문제의 판정이 전이 확인이 된다. */
    EXPLAINED,

    /** 힌트 없이 통과했다. 가장 무거운 증거가 된다. */
    VERIFIED,

    /**
     * 끝났지만 전이 증거는 되지 않았다.
     *
     * 통과하지 못했거나, 힌트를 보고 통과했거나, 설명을 쓰기 전에 통과했다. 셋 다
     * "옮겨졌다"를 말해 주지 못한다 — 실패가 아니라 **증거가 아닐 뿐**이다.
     */
    UNVERIFIED,
}
