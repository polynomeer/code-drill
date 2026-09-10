package dev.codedrill.controlplane.workspace

import dev.codedrill.judge.protocol.MutantOutcome
import dev.codedrill.judge.protocol.MutationStatus
import java.time.Instant
import java.util.UUID

/**
 * 내 테스트가 무엇을 잡고 무엇을 놓치는가 (PRD FR-804).
 *
 * 시험 실행([TrialRun])과 나란히 서지만 재는 것이 반대다. 시험 실행은 케이스를 잣대로
 * 삼아 **코드**를 재고, 이것은 코드(저작자의 오답)를 잣대로 삼아 **케이스**를 잰다.
 *
 * 판정이 아니다. 다만 시험 실행과 달리 **역량 증거는 남긴다** — 여기서 드러나는 것은
 * 코드가 맞았는지가 아니라 무엇을 시험해야 하는지 아는가이고, 그것이 검증 역량군
 * (테스트 설계·엣지케이스·반례)에서 재려던 바로 그것이다 (§4.2).
 */
data class MutationEvaluation(
    val id: UUID,
    val userId: String,
    val problemId: String,
    val problemVersion: Int,
    val cases: List<TrialCase>,
    val status: MutationRunStatus,
    val message: String?,
    /** 기대 출력이 실제 정답과 다른 케이스 번호 (1부터). */
    val mistakenCases: List<Int>,
    val outcomes: List<MutantOutcome>,
    val createdAt: Instant,
)

/**
 * 실행 상태.
 *
 * 실행 영역의 [MutationStatus] 를 그대로 쓰지 않는다. 저기에는 큐에 올렸을 뿐인 상태가
 * 없고 — 실행 영역은 자기가 받은 것만 알므로 있을 수도 없다 — 그 상태가 없으면 결과를
 * 잃어버렸을 때 사용자에게 아무것도 말할 수 없다.
 */
enum class MutationRunStatus {
    /** 큐에 올렸다. 잃어버리면 여기서 멈춘다 — 다시 누르면 된다. */
    PENDING,

    COMPLETED,

    /** 기대 출력을 적은 케이스가 없었거나 대조할 오답이 없었다. 실패가 아니다. */
    NO_CASES,

    /** 정답이나 오답이 돌지 않았다. 사용자 잘못이 아니다. */
    FAILED;

    companion object {
        fun of(status: MutationStatus) = when (status) {
            MutationStatus.COMPLETED -> COMPLETED
            MutationStatus.NO_CASES -> NO_CASES
            MutationStatus.FAILED -> FAILED
        }
    }
}
