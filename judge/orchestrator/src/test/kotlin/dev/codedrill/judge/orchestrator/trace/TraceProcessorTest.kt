package dev.codedrill.judge.orchestrator.trace

import dev.codedrill.judge.protocol.TargetKind
import dev.codedrill.judge.protocol.TraceCapture
import dev.codedrill.judge.protocol.TraceEvent
import dev.codedrill.judge.protocol.TraceEventType
import dev.codedrill.judge.protocol.TraceManifest
import dev.codedrill.judge.protocol.TraceStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 시각화 검증 (기술 설계서 §14.3).
 *
 * 축약이 핵심 의미를 보존하는지, 청크 경계가 정확한지, 못 믿을 트레이스를 걸러 내는지를
 * 고정한다. 트레이스는 부가 기능이지만 **틀린 트레이스는 없는 것보다 나쁘다** — 사용자가
 * 자기 코드를 오해하게 만든다.
 */
class TraceProcessorTest {

    @Test
    fun `정상 트레이스는 청크와 요약을 만든다`() {
        val result = TraceProcessor.process("t1", "s1", capture(events(1_200)))

        assertEquals(TraceStatus.READY, result.manifest.status)
        assertEquals(1_200, result.manifest.eventCount)
        assertEquals(3, result.chunks.size, "500 개씩 3 청크")
        assertEquals(listOf(500, 500, 200), result.chunks.map { it.events.size })
    }

    @Test
    fun `청크 목차의 seq 범위가 실제 내용과 일치한다`() {
        val result = TraceProcessor.process("t1", "s1", capture(events(1_200)))

        // 목차만 보고 "이 위치는 몇 번 청크"를 계산할 수 있어야 한다 (§7.5).
        for (ref in result.manifest.chunks) {
            val chunk = result.chunks[ref.index]
            assertEquals(chunk.events.first().seq, ref.firstSeq)
            assertEquals(chunk.events.last().seq, ref.lastSeq)
            assertEquals(chunk.events.size, ref.eventCount)
        }
    }

    @Test
    fun `요약은 예산을 넘지 않고 시간순을 유지한다`() {
        val result = TraceProcessor.process("t1", "s1", capture(events(5_000)))
        val summary = result.manifest.summary

        assertTrue(summary.size <= TraceManifest.SUMMARY_BUDGET, "요약 크기: ${summary.size}")
        assertEquals(summary.sortedBy { it.seq }, summary, "요약은 타임라인이므로 순서가 지켜져야 한다")
    }

    @Test
    fun `축약해도 가장 중요한 이벤트는 남는다`() {
        // 마지막에 MATCH(중요도 3)를 하나 둔다. 앞에서 자르는 방식이면 이것이 사라진다.
        val many = events(5_000) + TraceEvent(
            seq = 5_001,
            logicalTime = 5_001,
            eventType = TraceEventType.MATCH,
            targetKind = TargetKind.ARRAY,
            targetRef = "42",
            after = "99",
            importance = 3,
        )

        val summary = TraceProcessor.process("t1", "s1", capture(many)).manifest.summary

        assertTrue(
            summary.any { it.eventType == TraceEventType.MATCH },
            "답을 찾은 순간이 요약에서 사라지면 리플레이의 의미가 없다",
        )
    }

    @Test
    fun `예산 안이면 요약이 원본과 같다`() {
        val small = events(300)

        val summary = TraceProcessor.process("t1", "s1", capture(small)).manifest.summary

        assertEquals(small, summary)
    }

    @Test
    fun `순번이 뒤엉킨 트레이스는 INVALID 다`() {
        val broken = listOf(event(1), event(5), event(3))

        val manifest = TraceProcessor.process("t1", "s1", capture(broken)).manifest

        assertEquals(TraceStatus.INVALID, manifest.status)
        assertTrue(assertNotNull(manifest.diagnostics).contains("단조 증가"))
        assertTrue(manifest.chunks.isEmpty(), "못 믿을 트레이스는 청크를 만들지 않는다")
    }

    @Test
    fun `스키마 버전이 다르면 INVALID 다`() {
        val old = capture(events(10)).copy(schemaVersion = "1.0")

        val manifest = TraceProcessor.process("t1", "s1", old).manifest

        assertEquals(TraceStatus.INVALID, manifest.status)
    }

    @Test
    fun `이벤트가 없으면 EMPTY 이고 사유가 남는다`() {
        val manifest = TraceProcessor.process("t1", "s1", capture(emptyList())).manifest

        assertEquals(TraceStatus.EMPTY, manifest.status)
        assertNotNull(manifest.diagnostics)
    }

    @Test
    fun `같은 입력은 같은 결과를 낸다`() {
        val input = capture(events(2_000))

        val first = TraceProcessor.process("t1", "s1", input)
        val second = TraceProcessor.process("t1", "s1", input)

        assertEquals(first.manifest, second.manifest)
        assertEquals(first.chunks, second.chunks)
    }

    // --- 픽스처 ---

    private fun capture(events: List<TraceEvent>) = TraceCapture(
        caseId = "sample/01",
        events = events,
        truncated = false,
        diagnostics = null,
    )

    /** 중요도를 섞어 만든다. 축약이 무엇을 남기는지 보려면 분포가 필요하다. */
    private fun events(count: Int): List<TraceEvent> = (1..count).map { seq ->
        when (seq % 10) {
            0 -> event(seq.toLong(), TraceEventType.SWAP, 2)
            5 -> event(seq.toLong(), TraceEventType.COMPARE, 2)
            else -> event(seq.toLong(), TraceEventType.VISIT, 1)
        }
    }

    private fun event(
        seq: Long,
        type: TraceEventType = TraceEventType.VISIT,
        importance: Int = 1,
    ) = TraceEvent(
        seq = seq,
        logicalTime = seq,
        eventType = type,
        targetKind = type.kind,
        targetRef = seq.toString(),
        after = seq.toString(),
        importance = importance,
    )
}
