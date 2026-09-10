package dev.codedrill.controlplane.submission.trace

import dev.codedrill.judge.protocol.ShrinkStatus
import java.time.Instant
import java.util.UUID

/**
 * 최소 반례 (§6.3, PRD §3.4 검증군 증거).
 *
 * **떨어진 입력 그대로는 배울 것이 없다.** 20만 원소에서 틀렸다는 사실은 원인을 가리키지
 * 않지만, 세 원소로 줄인 입력은 대개 원인 그 자체다.
 */
data class Counterexample(
    val submissionId: UUID,
    val userId: String,
    val status: CounterexampleStatus,
    val message: String?,
    val args: List<Any>?,
    /** 이 입력에서 내 코드가 내놓은 것. */
    val actual: String?,
    /** 이 입력의 정답. */
    val expected: String?,
    val originalSize: Int?,
    val minimalSize: Int?,
    val rounds: Int?,
    val createdAt: Instant,
)

enum class CounterexampleStatus {
    /** 큐에 올렸다. 이 시스템에서 가장 오래 걸리는 작업이라 기다림이 길다. */
    PENDING,

    FOUND,

    /** 이 입력에서는 참조와 같은 답을 냈다. 값이 아니라 시간·메모리로 떨어진 경우다. */
    NOT_REPRODUCED,

    FAILED;

    companion object {
        fun of(status: ShrinkStatus) = when (status) {
            ShrinkStatus.FOUND -> FOUND
            ShrinkStatus.NOT_REPRODUCED -> NOT_REPRODUCED
            ShrinkStatus.FAILED -> FAILED
        }
    }
}
