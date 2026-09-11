// 검증용 정답 (§6.1 solutions/). 답에 대한 이분 탐색.
fun minCapacity(weights: IntArray, days: Int): Int {
    fun daysNeeded(cap: Int): Int {
        var used = 1
        var load = 0
        for (w in weights) {
            if (load + w > cap) { used += 1; load = 0 }
            load += w
        }
        return used
    }
    var lo = weights.max()
    var hi = weights.sum()
    while (lo < hi) {
        val mid = lo + (hi - lo) / 2
        Drill.pointer("lo", lo)
        Drill.pointer("hi", hi)
        Drill.compare(mid, days)
        if (daysNeeded(mid) <= days) hi = mid else lo = mid + 1
    }
    return lo
}
