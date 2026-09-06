package dev.codedrill.judge.protocol

/**
 * 실행 트레이스 (기술 설계서 §7).
 *
 * 이벤트는 코드 줄이 아니라 **의미 단위**로 남긴다. 자동 추론은 하지 않고 문제별 계측
 * 규약과 SDK 를 쓴다 (§0.3). 사용자가 SDK 를 부르지 않으면 트레이스가 비어 있는 것이
 * 정상이며, 그 사실을 진단으로 남긴다.
 *
 * 트레이스는 부가 기능이다. 장애가 나도 판정과 제출 이력은 정상 제공되어야 한다 (§1.2).
 */

/**
 * Trace Event Envelope (§7.2).
 *
 * [importance] 는 요약 타임라인을 만들 때의 선별 기준이다 (0..3). 예산을 넘겨 잘라야 할 때
 * 무엇을 남길지 정하는 유일한 근거이므로, SDK 가 이벤트마다 성실히 채워야 한다.
 */
data class TraceEvent(
    val seq: Long,
    /** 결정적 논리 시각. 슬라이스는 seq 와 같고, 병렬 실행이 들어오면 갈라진다. */
    val logicalTime: Long,
    val eventType: TraceEventType,
    /** 대상 자료구조의 종류. 렌더러는 이 값으로 고른다 (§7.5 플러그인 레지스트리). */
    val targetKind: TargetKind,
    /** 대상 참조. 배열은 인덱스, 그래프는 정점 id 처럼 종류마다 뜻이 다르다. */
    val targetRef: String,
    val before: String? = null,
    val after: String? = null,
    val importance: Int = 1,
    /** 코드 구간 연결용 줄 번호. SDK 가 알 수 있을 때만 채운다 (§7.2 sourceSpan). */
    val sourceLine: Int? = null,
    /** 렌더러 확장 속성. 크기를 제한해 트레이스가 임의 데이터 통로가 되지 않게 한다. */
    val attributes: Map<String, String> = emptyMap(),
)

/**
 * 의미 이벤트 종류.
 *
 * 자료구조별로 나눈다. 하나의 범용 이벤트에 attributes 로 구분을 넣으면 렌더러가 문자열
 * 비교로 분기하게 되고, 새 종류를 추가할 때 어디를 고쳐야 하는지 알 수 없게 된다.
 */
enum class TraceEventType(val kind: TargetKind, val defaultImportance: Int) {
    // 배열과 포인터
    VISIT(TargetKind.ARRAY, 1),
    COMPARE(TargetKind.ARRAY, 2),
    SWAP(TargetKind.ARRAY, 2),
    WRITE(TargetKind.ARRAY, 2),
    POINTER(TargetKind.ARRAY, 1),

    // 스택
    PUSH(TargetKind.STACK, 2),
    POP(TargetKind.STACK, 2),

    // 큐
    ENQUEUE(TargetKind.QUEUE, 2),
    DEQUEUE(TargetKind.QUEUE, 2),

    // 그래프
    NODE(TargetKind.GRAPH, 2),
    EDGE(TargetKind.GRAPH, 1),

    // 재귀
    CALL(TargetKind.CALL, 2),
    RETURN(TargetKind.CALL, 2),

    /** 답을 확정했다. 어느 자료구조에서든 가장 중요한 순간이다. */
    MATCH(TargetKind.ARRAY, 3),
}

/** 렌더러가 붙는 단위 (§1.1 배열·포인터·스택·큐·기초 그래프·재귀). */
enum class TargetKind { ARRAY, STACK, QUEUE, GRAPH, CALL }

/**
 * 가공이 끝난 트레이스의 목차 (§7.4 trace manifest).
 *
 * 클라이언트는 이것과 [summary] 를 먼저 받고, 상세 청크는 필요한 위치 주변만 내려받는다
 * (§7.5). manifest 자체는 32KB 안에 들어와야 하므로 이벤트 본문을 담지 않는다.
 */
data class TraceManifest(
    val schemaVersion: String = SCHEMA_VERSION,
    val traceId: String,
    val submissionId: String,
    val caseId: String,
    val status: TraceStatus,
    val eventCount: Int,
    /** 청크 목차. 각 청크가 담는 seq 범위를 알면 위치로 청크를 찾을 수 있다. */
    val chunks: List<ChunkRef>,
    /** 요약 타임라인. 중요도 높은 이벤트만 골라 1,000개 이하로 줄인다 (§7.4). */
    val summary: List<TraceEvent>,
    /** 잘렸는지. 조용히 버리지 않고 사실을 알린다 (§7.1). */
    val truncated: Boolean,
    val diagnostics: String? = null,
) {
    companion object {
        const val SCHEMA_VERSION = "2.0"

        /** 실행당 이벤트 상한. 넘으면 drop 이 아니라 잘렸다는 사실을 함께 알린다. */
        const val EVENT_BUDGET = 20_000

        /** 요약에 담을 최대 이벤트 수 (§7.4 summary 1,000 이벤트 이하). */
        const val SUMMARY_BUDGET = 1_000

        /** 청크 하나에 담을 이벤트 수. 청크가 작을수록 seek 비용이 낮고 요청은 잦아진다. */
        const val CHUNK_SIZE = 500
    }
}

/**
 * 청크 목차 항목.
 *
 * [firstSeq]/[lastSeq] 로 "이 위치는 몇 번 청크인가"를 계산한다. 클라이언트가 청크를
 * 다 받아 보고서야 범위를 아는 구조라면 seek 마다 헛된 요청이 생긴다.
 */
data class ChunkRef(val index: Int, val firstSeq: Long, val lastSeq: Long, val eventCount: Int)

/** 이벤트 청크 (§7.4 raw event chunk). */
data class TraceChunk(
    val schemaVersion: String = TraceManifest.SCHEMA_VERSION,
    val traceId: String,
    val index: Int,
    val events: List<TraceEvent>,
)

/**
 * 트레이스 상태 (§7.3 Validation 실패 시 처리).
 *
 * 검증에 실패해도 판정은 그대로 유지한다. 트레이스가 없거나 깨진 것이 오답을 만들지 않는다.
 */
enum class TraceStatus {
    /** 정상적으로 가공됐다. */
    READY,

    /** 계측 호출이 없어 이벤트가 하나도 없다. 오류가 아니다. */
    EMPTY,

    /** 스키마·순서·크기 검증에 실패했다. 판정은 유지하고 리플레이만 제공하지 않는다. */
    INVALID,
}

/** 트레이스가 준비됐다. 멱등 키는 `traceId + schemaVersion` 다 (§3.3). */
data class TraceReady(
    val schemaVersion: String = TraceManifest.SCHEMA_VERSION,
    val submissionId: String,
    val executionId: String,
    val correlationId: String,
    val manifest: TraceManifest,
    val chunks: List<TraceChunk>,
)

/**
 * Runner 가 수집해 올려 보내는 가공 전 이벤트 (§7.3 Collection).
 *
 * 검증·축약·청킹은 Runner 밖에서 한다. 샌드박스 안에서 도는 코드가 많을수록 실행 시간이
 * 판정에 섞이고, 계측 비용이 사용자 코드 시간으로 보이게 된다.
 */
data class TraceCapture(
    val schemaVersion: String = TraceManifest.SCHEMA_VERSION,
    val caseId: String,
    val events: List<TraceEvent>,
    val truncated: Boolean,
    val diagnostics: String?,
)

/**
 * 실행 모드 (§7.1).
 *
 * 공식 채점과 학습용 트레이스를 분리한다. 계측 오버헤드가 판정 시간에 섞이면 TIME_LIMIT
 * 이 계측 비용 때문에 나는 것인지 알 수 없게 된다.
 */
enum class ExecutionMode { JUDGE, TRACE }
