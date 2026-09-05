package dev.codedrill.judge.protocol

/**
 * 제어 영역과 실행 영역 사이의 메시지 계약 (기술 설계서 §3.3 내부 이벤트).
 *
 * 두 영역은 별도 배포 단위이므로 서로의 내부 모델을 알지 못한다. 이 파일이 둘이
 * 공유하는 전부다. 소비자는 N/N-1 스키마를 함께 지원하고 모르는 필드는 무시한다 (§15.3).
 */

/** 제출이 큐에 올랐다. 멱등 키는 `submissionId + attempt` 다. */
data class SubmissionQueued(
    val schemaVersion: String = SCHEMA_VERSION,
    val submissionId: String,
    val correlationId: String,
    val problemId: String,
    val problemVersion: Int,
    val language: Language,
    /**
     * 슬라이스는 소스를 메시지에 그대로 싣는다. 운영에서는 오브젝트 스토어에 올리고
     * 참조와 digest 만 실어야 한다 (§8.3).
     */
    val source: String,
    /** 판정이 끝난 뒤 학습용 트레이스를 이어서 만들지 (§9.3 requestTrace). */
    val requestTrace: Boolean = false,
) {
    companion object {
        const val SCHEMA_VERSION = "1.0"
    }
}

/** 채점이 진행됐다. 멱등 키는 `submissionId + seq` 다. */
data class JudgeProgressed(
    val schemaVersion: String = SubmissionQueued.SCHEMA_VERSION,
    val submissionId: String,
    val correlationId: String,
    val seq: Long,
    val status: JudgeStatus,
)

/**
 * 채점이 끝났다. 멱등 키는 `executionId` 다.
 *
 * 제어 영역은 이 메시지로 제출을 종료 상태로 옮긴다. 같은 executionId 가 다시 오면
 * 무시한다.
 */
data class JudgeCompleted(
    val schemaVersion: String = SubmissionQueued.SCHEMA_VERSION,
    val submissionId: String,
    val executionId: String,
    val correlationId: String,
    val verdict: Verdict,
    val score: Int,
    val compileLog: String?,
    val groups: List<CompletedGroup>,
)

data class CompletedGroup(
    val groupId: String,
    val verdict: Verdict,
    val score: Int,
    val maxScore: Int,
    /** 숨은 그룹은 케이스 내역을 비워 보낸다. 제어 영역이 그대로 내려보내도 안전해야 한다. */
    val cases: List<TestCaseResult>,
)

/** 채점 진행 단계. 제출 상태 머신(§4.2)의 실행 영역 쪽 투영이다. */
enum class JudgeStatus { LEASED, COMPILING, RUNNING, AGGREGATING }
