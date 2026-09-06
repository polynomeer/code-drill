package dev.codedrill.judge.orchestrator.trace

import dev.codedrill.judge.protocol.ChunkRef
import dev.codedrill.judge.protocol.TraceCapture
import dev.codedrill.judge.protocol.TraceChunk
import dev.codedrill.judge.protocol.TraceEvent
import dev.codedrill.judge.protocol.TraceManifest
import dev.codedrill.judge.protocol.TraceStatus

/**
 * Trace Processor (기술 설계서 §7.3).
 *
 * ```
 * Validation → Reduction → Chunking → (Delivery)
 * ```
 *
 * Runner 는 이벤트를 모으기만 하고 가공은 여기서 한다. 샌드박스 안에서 도는 일이 많을수록
 * 계측 비용이 사용자 코드의 실행 시간처럼 보이기 때문이다 (§7.1).
 *
 * 이 처리는 **판정과 완전히 독립**이다. 어떤 단계가 실패해도 트레이스 상태만 바뀌고
 * 제출의 판정은 그대로 유지된다 (§1.2, §12.2).
 *
 * 배포 단위는 아직 오케스트레이터 안이다. 트레이스 물량이 채점 지연에 영향을 주기
 * 시작하면 §2.2 대로 독립 컴포넌트로 떼어내야 한다.
 */
object TraceProcessor {

    data class Processed(val manifest: TraceManifest, val chunks: List<TraceChunk>)

    fun process(traceId: String, submissionId: String, capture: TraceCapture): Processed {
        val validation = validate(capture)
        if (validation != null) {
            return empty(traceId, submissionId, capture, TraceStatus.INVALID, validation)
        }
        if (capture.events.isEmpty()) {
            return empty(
                traceId,
                submissionId,
                capture,
                TraceStatus.EMPTY,
                capture.diagnostics ?: "계측 호출이 없어 트레이스가 비어 있다",
            )
        }

        val events = capture.events.sortedBy { it.seq }
        val chunks = chunk(traceId, events)

        return Processed(
            manifest = TraceManifest(
                traceId = traceId,
                submissionId = submissionId,
                caseId = capture.caseId,
                status = TraceStatus.READY,
                eventCount = events.size,
                chunks = chunks.map {
                    ChunkRef(
                        index = it.index,
                        firstSeq = it.events.first().seq,
                        lastSeq = it.events.last().seq,
                        eventCount = it.events.size,
                    )
                },
                summary = reduce(events),
                truncated = capture.truncated,
                diagnostics = capture.diagnostics,
            ),
            chunks = chunks,
        )
    }

    /**
     * 검증 (§7.3 Validation). 문제가 있으면 사유를, 없으면 null 을 돌려준다.
     *
     * 스키마가 다르거나 순서가 어긋난 트레이스를 그대로 내보내면, 클라이언트가 이상한
     * 상태를 그려 놓고 사용자는 자기 코드가 그런 줄 안다. 못 믿을 트레이스는 아예 주지
     * 않는 편이 낫다.
     */
    private fun validate(capture: TraceCapture): String? {
        if (capture.schemaVersion != TraceManifest.SCHEMA_VERSION) {
            return "스키마 버전이 다르다: ${capture.schemaVersion}"
        }
        if (capture.events.size > TraceManifest.EVENT_BUDGET) {
            return "이벤트 수가 예산을 넘었다: ${capture.events.size}"
        }

        var previous = 0L
        for (event in capture.events) {
            if (event.seq <= previous) return "순번이 단조 증가하지 않는다: ${event.seq}"
            if (event.importance !in 0..3) return "importance 가 범위를 벗어났다: ${event.importance}"
            if (event.targetRef.length > MAX_REF_LENGTH) return "target 참조가 너무 길다"
            previous = event.seq
        }
        return null
    }

    /**
     * 축약 (§7.3 Reduction, §7.4 summary 1,000 이벤트 이하).
     *
     * 예산 안이면 전부 요약이다. 넘으면 **중요도 높은 이벤트를 먼저 남기고**, 같은
     * 중요도끼리는 시간축에 고르게 퍼지도록 고른다. 앞부분만 남기면 "답을 찾은 순간"처럼
     * 뒤쪽의 결정적 사건이 통째로 사라진다.
     *
     * 고른 뒤에는 반드시 seq 순으로 되돌린다. 요약은 타임라인이므로 순서가 뒤집히면
     * 그것만으로 못 쓰는 데이터가 된다.
     */
    private fun reduce(events: List<TraceEvent>): List<TraceEvent> {
        if (events.size <= TraceManifest.SUMMARY_BUDGET) return events

        val selected = mutableListOf<TraceEvent>()
        var remaining = TraceManifest.SUMMARY_BUDGET

        // 중요도 3 → 0 순으로 채운다.
        for (importance in 3 downTo 0) {
            if (remaining <= 0) break
            val bucket = events.filter { it.importance == importance }
            if (bucket.isEmpty()) continue

            if (bucket.size <= remaining) {
                selected += bucket
                remaining -= bucket.size
            } else {
                // 균등 샘플링. 앞에서 자르면 뒤쪽 사건이 통째로 사라진다.
                val step = bucket.size.toDouble() / remaining
                for (i in 0 until remaining) {
                    selected += bucket[(i * step).toInt()]
                }
                remaining = 0
            }
        }
        return selected.sortedBy { it.seq }
    }

    /**
     * 청킹 (§7.3 Chunking, §7.4 raw event chunk).
     *
     * 고정 크기로 자른다. 클라이언트는 manifest 의 seq 범위로 원하는 위치의 청크만
     * 내려받으므로, 긴 트레이스에서도 처음부터 다 받지 않아도 된다 (§7.5).
     */
    private fun chunk(traceId: String, events: List<TraceEvent>): List<TraceChunk> =
        events.chunked(TraceManifest.CHUNK_SIZE).mapIndexed { index, slice ->
            TraceChunk(traceId = traceId, index = index, events = slice)
        }

    private fun empty(
        traceId: String,
        submissionId: String,
        capture: TraceCapture,
        status: TraceStatus,
        diagnostics: String,
    ) = Processed(
        manifest = TraceManifest(
            traceId = traceId,
            submissionId = submissionId,
            caseId = capture.caseId,
            status = status,
            eventCount = 0,
            chunks = emptyList(),
            summary = emptyList(),
            truncated = capture.truncated,
            diagnostics = diagnostics,
        ),
        chunks = emptyList(),
    )

    private const val MAX_REF_LENGTH = 128
}
