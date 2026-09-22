// kind: PERFORMANCE
// 상태를 기억하지 않는다. target 500 에 후보 열 개면 지수다.
fun combinationSumCount(candidates: IntArray, target: Int): Int {
    val sorted = candidates.sorted()
    var count = 0
    fun go(i: Int, remaining: Int) {
        if (remaining == 0) { count += 1; return }
        for (j in i until sorted.size) {
            if (sorted[j] > remaining) break
            Drill.compare(j, remaining)
            go(j, remaining - sorted[j])
        }
    }
    go(0, target)
    return count
}
