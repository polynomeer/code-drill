package dev.codedrill.judge.protocol

/**
 * 실행 트레이스 (기술 설계서 §7).
 *
 * 이벤트는 코드 줄이 아니라 **의미 단위**(compare, visit, match)로 남긴다. 자동 추론은
 * 하지 않고 문제별 계측 규약과 SDK 를 쓴다 (§0.3). 그래서 사용자가 SDK 를 부르지 않으면
 * 트레이스가 비어 있는 것이 정상이며, 그 사실을 [TraceCapture.diagnostics] 에 남긴다.
 */
data class TraceCapture(
    val schemaVersion: String = SCHEMA_VERSION,
    val caseId: String,
    val events: List<TraceEvent>,
    /** 이벤트 예산을 넘겨 잘렸는지. 잘림을 감추지 않는 것이 §7.1 의 요구다. */
    val truncated: Boolean,
    val diagnostics: String?,
) {
    companion object {
        const val SCHEMA_VERSION = "1.0"

        /** 실행당 이벤트 상한. 넘으면 drop 이 아니라 잘렸다는 사실을 함께 알린다. */
        const val EVENT_BUDGET = 1_000
    }
}

/**
 * Trace Event Envelope (§7.2)의 슬라이스 범위.
 *
 * 원본 스키마의 traceId·sourceSpan·attributes·visibility 는 아직 채우지 않는다. 배열과
 * 포인터를 되짚는 데 필요한 최소 필드만 두고, 스키마 버전으로 나중에 넓힌다.
 */
data class TraceEvent(
    val seq: Long,
    val logicalTime: Long,
    val eventType: TraceEventType,
    /** 대상 참조. 슬라이스는 `array:<index>` 한 종류만 쓴다. */
    val target: String,
    val before: String?,
    val after: String?,
    /** 0..3. 요약 타임라인을 만들 때 선별 기준이 된다 (§7.2). */
    val importance: Int,
)

enum class TraceEventType { VISIT, COMPARE, MATCH }

/** 트레이스가 준비됐다. 멱등 키는 `traceId + schemaVersion` 다 (§3.3). */
data class TraceReady(
    val schemaVersion: String = TraceCapture.SCHEMA_VERSION,
    val submissionId: String,
    val executionId: String,
    val correlationId: String,
    val capture: TraceCapture,
)

/**
 * 실행 모드 (§7.1).
 *
 * 공식 채점과 학습용 트레이스를 분리한다. 계측 오버헤드가 판정 시간에 섞이면 TIME_LIMIT
 * 이 계측 비용 때문에 나는 것인지 알 수 없게 된다.
 */
enum class ExecutionMode { JUDGE, TRACE }
