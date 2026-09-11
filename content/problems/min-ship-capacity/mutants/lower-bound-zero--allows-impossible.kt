// kind: MISSING_EDGE_CASE
// 하한을 0 으로 잡는다. 가장 무거운 짐보다 작은 용량을 답으로 낼 수 있다.
fun minCapacity(weights: IntArray, days: Int): Int {
    fun daysNeeded(cap: Int): Int {
        var used = 1; var load = 0
        for (w in weights) { if (load + w > cap) { used += 1; load = 0 }; load += w }
        return used
    }
    var lo = 1; var hi = weights.sum()
    while (lo < hi) {
        val mid = lo + (hi - lo) / 2
        if (daysNeeded(mid) <= days) hi = mid else lo = mid + 1
    }
    return lo
}
