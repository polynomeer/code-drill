// kind: OFF_BY_ONE
// 가능한 용량을 찾았을 때 hi 를 mid - 1 로 줄인다. 답 자체를 구간에서 밀어낸다.
fun minCapacity(weights: IntArray, days: Int): Int {
    fun daysNeeded(cap: Int): Int {
        var used = 1; var load = 0
        for (w in weights) { if (load + w > cap) { used += 1; load = 0 }; load += w }
        return used
    }
    var lo = weights.max(); var hi = weights.sum()
    while (lo < hi) {
        val mid = lo + (hi - lo) / 2
        if (daysNeeded(mid) <= days) hi = mid - 1 else lo = mid + 1
    }
    return lo
}
