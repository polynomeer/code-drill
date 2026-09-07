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
    /** 몇 번째 판정인지. 재채점이 이 값을 올린다 (§4.2 INV-02). */
    val revision: Int = 1,
    val version: Long = 0,
    val createdAt: Instant = Instant.EPOCH,
)

/**
 * 지나간 판정 하나 (기술 설계서 §4.2 INV-02).
 *
 * 최초 판정도 여기에 들어간다. 재채점 결과만 남기면 "무엇에서 무엇으로 바뀌었나"의
 * 앞쪽 절반이 비어, 정작 필요한 비교를 할 수 없다.
 */
data class Judgement(
    val revision: Int,
    val executionId: String,
    val verdict: Verdict,
    val score: Int,
    /** 어느 재채점이 만든 판정인지. null 이면 사용자가 직접 제출한 판정이다. */
    val rejudgeJobId: String?,
    /** 현재 판정으로 반영됐는지. dry-run 은 이력에만 남는다. */
    val applied: Boolean,
    val createdAt: Instant,
)
