// 검증용 정답 (§6.1 solutions/). 크기별로 기억하는 재귀.
fun uniqueBstCount(n: Int): Int {
    val memo = IntArray(n + 1) { -1 }
    fun go(k: Int): Int {
        if (k <= 1) return 1
        if (memo[k] >= 0) return memo[k]
        var total = 0
        for (root in 1..k) { Drill.compare(root - 1, k - root); total += go(root - 1) * go(k - root) }
        memo[k] = total
        Drill.write(k, total)
        return total
    }
    return go(n)
}
