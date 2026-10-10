package ranges

// kind: WRONG_ALGORITHM
// 빼는 구간에 걸치는 구간을 통째로 지운다. 걸친 구간의 양옆 조각은 남아야 한다.

import java.util.TreeMap

/**
 * 구간 집합. 겹치지도 맞닿지도 않는 구간들을 시작점 → 끝점의 정렬된 맵으로 든다 — 더하기는 걸치는 구간을 모두 삼켜
 * 하나로, 빼기는 걸치는 구간을 잘라 양옆의 남는 조각만 남긴다.
 */
class RangeSet {

    private val spans = TreeMap<Long, Long>()

    private fun check(from: Long, to: Long) {
        require(from <= to) { "from must not be greater than to" }
    }

    fun add(from: Long, to: Long) {
        check(from, to)
        if (from == to) return
        var start = from
        var end = to
        // 시작이 from 이하인 마지막 구간이 from 에 닿거나 겹치면 함께 삼킨다(맞닿은 것도 합친다)
        val before = spans.floorEntry(from)
        if (before != null && before.value >= from) {
            start = before.key
            end = maxOf(end, before.value)
        }
        // 시작이 [start, end] 안인 구간을 모두 삼킨다 — 끝점 end 에서 시작하는 구간도 맞닿았으니 합친다
        while (true) {
            val next = spans.ceilingEntry(start) ?: break
            if (next.key > end) break
            end = maxOf(end, next.value)
            spans.remove(next.key)
        }
        spans[start] = end
    }

    fun remove(from: Long, to: Long) {
        check(from, to)
        if (from == to) return
        val before = spans.lowerEntry(from)
        if (before != null && before.value > from) spans.remove(before.key)
        while (true) {
            val next = spans.ceilingEntry(from) ?: break
            if (next.key >= to) break
            spans.remove(next.key)
        }
    }

    fun contains(x: Long): Boolean {
        val entry = spans.floorEntry(x) ?: return false
        return x < entry.value
    }

    fun covers(from: Long, to: Long): Boolean {
        check(from, to)
        if (from == to) return true
        val entry = spans.floorEntry(from) ?: return false
        return to <= entry.value
    }

    fun spans(): List<Span> = spans.map { Span(it.key, it.value) }

    fun size(): Long = spans.entries.sumOf { it.value - it.key }
}
