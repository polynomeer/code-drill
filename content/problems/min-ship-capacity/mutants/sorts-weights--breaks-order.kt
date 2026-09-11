// kind: WRONG_ALGORITHM
// 짐을 정렬해 싣는다. 순서를 바꾸면 안 된다는 조건을 무시했다.
fun minCapacity(weights: IntArray, days: Int): Int {
    val sorted = weights.sortedArray()
    fun daysNeeded(cap: Int): Int {
        var used = 1; var load = 0
        for (w in sorted) { if (load + w > cap) { used += 1; load = 0 }; load += w }
        return used
    }
    var lo = sorted.last(); var hi = sorted.sum()
    while (lo < hi) {
        val mid = lo + (hi - lo) / 2
        if (daysNeeded(mid) <= days) hi = mid else lo = mid + 1
    }
    return lo
}
