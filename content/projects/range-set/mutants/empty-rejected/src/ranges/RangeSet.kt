package ranges

// kind: MISSING_EDGE_CASE
// 빈 구간(from == to)도 거부한다. 빈 구간은 아무 일도 하지 않고, 덮는지 물으면 참이다.

import java.util.TreeMap

/**
 * 구간 집합. 겹치지도 맞닿지도 않는 구간들을 시작점 → 끝점의 정렬된 맵으로 든다 — 더하기는 걸치는 구간을 모두 삼켜
 * 하나로, 빼기는 걸치는 구간을 잘라 양옆의 남는 조각만 남긴다.
 */
class RangeSet {

    private val spans = TreeMap<Long, Long>()

    private fun check(from: Long, to: Long) {
        require(from < to) { "from must be less than to" }
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
        if (before != null && before.value > from) {
            // from 앞에서 시작해 걸치는 구간 — 왼쪽 조각을 남기고, 오른쪽으로 to 를 넘으면 그 조각도 남긴다
            spans[before.key] = from
            if (before.value > to) spans[to] = before.value
        }
        while (true) {
            val next = spans.ceilingEntry(from) ?: break
            if (next.key >= to) break
            spans.remove(next.key)
            if (next.value > to) spans[to] = next.value
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
