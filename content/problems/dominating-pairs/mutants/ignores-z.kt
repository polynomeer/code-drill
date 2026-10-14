// kind: WRONG_ALGORITHM
// 등번호와 y 두 기준만 본다. z 에서도 앞서야 한다.
fun dominatingPairs(ys: IntArray, zs: IntArray): Int {
    val n = ys.size
    val tree = IntArray(n + 1)
    var total = 0L
    for (j in 0 until n) {
        var i = ys[j]; var s = 0
        while (i > 0) { s += tree[i]; i -= i and -i }
        total += s
        i = ys[j] + 1
        while (i <= n) { tree[i] += 1; i += i and -i }
    }
    return (total % 1_000_000_007).toInt()
}
