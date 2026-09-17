// kind: OFF_BY_ONE
// 루트를 1..k-1 만 잡는다. 마지막 값이 루트인 트리를 빠뜨린다.
fun uniqueBstCount(n: Int): Int {
    val memo = IntArray(n + 1) { -1 }
    fun go(k: Int): Int {
        if (k <= 1) return 1
        if (memo[k] >= 0) return memo[k]
        var total = 0
        for (root in 1 until k) total += go(root - 1) * go(k - root)
        memo[k] = total
        return total
    }
    return go(n)
}
