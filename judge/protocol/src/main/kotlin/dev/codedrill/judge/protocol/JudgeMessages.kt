package dev.codedrill.judge.protocol

import java.time.Instant

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
    /**
     * 아웃박스에 커밋된 시각. 큐 대기 시간(§12.1 Queue wait)의 기준점이다.
     *
     * 실행 영역은 제어 영역의 시계를 볼 수 없으므로, 재는 쪽이 아니라 **찍는 쪽**이
     * 시각을 실어 보내야 한다. 이전 버전 메시지에는 없으므로 null 을 허용하고, 없으면
     * 측정을 건너뛴다 (§15.3 N/N-1).
     */
    val queuedAt: Instant? = null,
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

/**
 * Runner 가 살아서 이 실행을 붙들고 있다 (기술 설계서 §4.3).
 *
 * 임대 만료는 "워커가 죽었다"를 뜻해야 한다. 그런데 요청이 브로커 큐에서 차례를
 * 기다리는 시간도 임대 시간에 들어가면, 밀린 것뿐인 멀쩡한 실행이 유실로 오해받아
 * 다시 돌고 결국 SYSTEM_ERROR 로 끝난다 — 바쁠 때만 정답이 틀리게 나오는, 가장 나쁜
 * 종류의 오판이다.
 *
 * 그래서 실행을 **집어 든 워커만** 임대를 연장한다. 토큰이 실려 있으므로 이미
 * 무효가 된 워커의 심장 박동은 현재 임대를 살려 두지 못한다.
 */
data class ExecutionHeartbeat(
    val schemaVersion: String = SubmissionQueued.SCHEMA_VERSION,
    val submissionId: String,
    val executionId: String,
    val attempt: Int,
    val fencingToken: FencingToken,
)
