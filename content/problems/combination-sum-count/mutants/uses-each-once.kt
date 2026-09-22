// kind: MISSING_EDGE_CASE
// 후보를 한 번씩만 쓴다. 다시 쓸 수 있어야 한다.
fun combinationSumCount(candidates: IntArray, target: Int): Int {
    val sorted = candidates.sorted()
    val memo = Array(sorted.size) { IntArray(target + 1) { -1 } }
    fun go(i: Int, remaining: Int): Int {
        if (remaining == 0) return 1
        if (i == sorted.size || remaining < 0) return 0
        if (memo[i][remaining] >= 0) return memo[i][remaining]
        val total = go(i + 1, remaining) + go(i + 1, remaining - sorted[i])
        memo[i][remaining] = total
        return total
    }
    return go(0, target)
}
