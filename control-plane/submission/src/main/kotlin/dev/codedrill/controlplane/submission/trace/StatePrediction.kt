package dev.codedrill.controlplane.submission.trace

import java.time.Instant
import java.util.UUID

/**
 * 리플레이 중 다음 이벤트 예측 (PRD FR-805).
 *
 * > 리플레이 중 다음 상태 예측과 최초 분기 진단을 지원합니다. 선택·근거·결과가
 * > 코드·입력·이벤트 시점과 연결됩니다.
 *
 * **자기 코드가 다음에 무엇을 할지 모른 채로 재생을 보는 것은 관람이지 학습이 아니다.**
 * 멈춰 세우고 한 번 말하게 하면, 틀린 그 자리가 곧 자기가 몰랐던 자리다.
 *
 * 한 자리는 한 번만 맞힐 수 있다. 틀린 뒤 답을 보고 다시 누르는 것은 예측이 아니라
 * 받아쓰기이고, 그것을 세면 증거가 통째로 거짓이 된다.
 */
data class StatePrediction(
    val id: UUID,
    val userId: String,
    val submissionId: UUID,
    /** 몇 번째 이벤트를 맞히려 했나 (1부터). */
    val step: Int,
    val predicted: String,
    val actual: String,
    val correct: Boolean,
    val rationale: String?,
    val createdAt: Instant,
)
