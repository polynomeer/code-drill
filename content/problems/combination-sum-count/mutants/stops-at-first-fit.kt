// kind: WRONG_BRANCH
// 남은 합이 0 이 되는 가지를 찾으면 그 후보를 더 시도하지 않는다.
fun combinationSumCount(candidates: IntArray, target: Int): Int {
    val sorted = candidates.sorted()
    var count = 0
    fun go(i: Int, remaining: Int) {
        if (remaining == 0) { count += 1; return }
        for (j in i until sorted.size) {
            if (sorted[j] > remaining) break
            go(j, remaining - sorted[j])
            if (remaining - sorted[j] == 0) return
        }
    }
    go(0, target)
    return count
}
