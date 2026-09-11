// kind: PERFORMANCE
// 용량을 하한부터 하나씩 올려 본다. 답까지 O(합 × n).
fun minCapacity(weights: IntArray, days: Int): Int {
    fun daysNeeded(cap: Int): Int {
        var used = 1; var load = 0
        for (i in weights.indices) { Drill.visit(i, weights[i]); if (load + weights[i] > cap) { used += 1; load = 0 }; load += weights[i] }
        return used
    }
    var cap = weights.max()
    while (daysNeeded(cap) > days) cap += 1
    return cap
}
