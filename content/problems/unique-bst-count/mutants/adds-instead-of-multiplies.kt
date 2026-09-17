// kind: WRONG_ALGORITHM
// 왼쪽과 오른쪽의 모양 수를 곱하지 않고 더한다.
fun uniqueBstCount(n: Int): Int {
    val memo = IntArray(n + 1) { -1 }
    fun go(k: Int): Int {
        if (k <= 1) return 1
        if (memo[k] >= 0) return memo[k]
        var total = 0
        for (root in 1..k) total += go(root - 1) + go(k - root)
        memo[k] = total
        return total
    }
    return go(n)
}
