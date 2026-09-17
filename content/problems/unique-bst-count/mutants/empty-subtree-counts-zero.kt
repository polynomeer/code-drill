// kind: MISSING_EDGE_CASE
// 빈 서브트리의 모양 수를 0 으로 둔다. 한쪽이 비는 루트가 전부 사라진다.
fun uniqueBstCount(n: Int): Int {
    if (n == 0) return 1
    val memo = IntArray(n + 1) { -1 }
    fun go(k: Int): Int {
        if (k == 0) return 0
        if (k == 1) return 1
        if (memo[k] >= 0) return memo[k]
        var total = 0
        for (root in 1..k) total += go(root - 1) * go(k - root)
        memo[k] = total
        return total
    }
    return go(n)
}
