// 검증용 정답 (§6.1 solutions/). (후보 자리, 남은 합) 을 기억하는 재귀.
fun combinationSumCount(candidates: IntArray, target: Int): Int {
    val sorted = candidates.sorted()
    val memo = Array(sorted.size) { IntArray(target + 1) { -1 } }
    fun go(i: Int, remaining: Int): Int {
        if (remaining == 0) { Drill.write(0, 1); return 1 }
        if (i == sorted.size || remaining < 0) return 0
        if (memo[i][remaining] >= 0) return memo[i][remaining]
        Drill.compare(i, remaining)
        val total = go(i + 1, remaining) + go(i, remaining - sorted[i])
        memo[i][remaining] = total
        return total
    }
    return go(0, target)
}
