package dev.codedrill.controlplane.submission

import dev.codedrill.judge.protocol.Verdict
import java.time.Instant
import java.util.UUID

/**
 * 제출 (기술 설계서 §8.1).
 *
 * 종료된 제출은 불변이다. 재채점은 이 행을 되돌리지 않고 새 revision 을 만든다.
 * [version] 은 상태 전이를 낙관적 갱신으로 적용하기 위한 컬럼이다 (§3.2).
 */
data class Submission(
    val id: UUID,
    val userId: String,
    val idempotencyKey: String,
    val problemId: String,
    val problemVersion: Int,
    val language: String,
    val status: SubmissionStatus,
    val verdict: Verdict? = null,
    val score: Int? = null,
    val compileLog: String? = null,
    val groupsJson: String? = null,
    val version: Long = 0,
    val createdAt: Instant = Instant.EPOCH,
)
