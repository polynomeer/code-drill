package dev.codedrill.controlplane.workspace

import dev.codedrill.judge.protocol.ArenaResult
import dev.codedrill.judge.protocol.ArenaStatus
import java.time.Instant
import java.util.UUID

/**
 * 아레나 시도 (기획서 §8.3).
 *
 * 변이 평가([MutationEvaluation])와 같은 재료로 반대 놀이를 한다. 저기서는 내 테스트가
 * 얼마나 잡는지를 재고, 여기서는 **입력 하나로 특정 오답을 깨뜨린다.** 오답의 이름과
 * 소스가 보이는 것이 다르고, 그것이 허용되는 이유는 이 문제를 맞힌 사람에게만 열리기
 * 때문이다.
 */
data class ArenaAttempt(
    val id: UUID,
    val userId: String,
    val problemId: String,
    val args: List<Any>,
    val status: ArenaAttemptStatus,
    val message: String?,
    val results: List<ArenaResult>,
    val createdAt: Instant,
)

enum class ArenaAttemptStatus {
    PENDING, COMPLETED, INVALID_INPUT, FAILED;

    companion object {
        fun of(status: ArenaStatus) = when (status) {
            ArenaStatus.COMPLETED -> COMPLETED
            ArenaStatus.INVALID_INPUT -> INVALID_INPUT
            ArenaStatus.FAILED -> FAILED
        }
    }
}

/** 기록판 한 줄 — 오답 하나를 누가 얼마나 작게 깨뜨렸나. */
data class ArenaRecord(
    val mutantName: String,
    val breakers: Int,
    val smallestSize: Int?,
    /** 가장 작은 반례가 내 것인가. 이름은 내보내지 않는다 (§8.5 평판 정책 전). */
    val smallestIsMine: Boolean,
    val firstIsMine: Boolean,
    /** 내가 깨뜨린 가장 작은 크기. 못 깨뜨렸으면 null. */
    val myBest: Int?,
)
