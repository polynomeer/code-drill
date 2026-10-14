// kind: WRONG_BRANCH
// 맨 위에서 한 번만 반을 갈라 가로지르는 쌍만 센다. 각 반 안의 쌍도 재귀로 세야 한다.
fun dominatingPairs(ys: IntArray, zs: IntArray): Int {
    val n = ys.size
    val mid = n / 2
    var total = 0L
    val left = (0 until mid).sortedBy { ys[it] }
    val right = (mid until n).sortedBy { ys[it] }
    val tree = IntArray(n + 1)
    var p = 0
    for (j in right) {
        while (p < left.size && ys[left[p]] < ys[j]) { var i = zs[left[p]] + 1; while (i <= n) { tree[i] += 1; i += i and -i }; p += 1 }
        var i = zs[j]; var s = 0
        while (i > 0) { s += tree[i]; i -= i and -i }
        total += s
    }
    return (total % 1_000_000_007).toInt()
}
