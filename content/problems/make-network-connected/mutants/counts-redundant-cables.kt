// kind: WRONG_ALGORITHM
// 여분 케이블 수를 답한다. 필요한 것은 무리 수 − 1 이다.
fun makeNetworkConnected(n: Int, cables: IntArray): Int {
    val m = cables.size / 2
    if (m < n - 1) return -1
    val parent = IntArray(n) { it }
    fun find(x: Int): Int { var r = x; while (parent[r] != r) { parent[r] = parent[parent[r]]; r = parent[r] }; return r }
    var redundant = 0
    for (i in 0 until m) { val a = find(cables[2 * i]); val b = find(cables[2 * i + 1]); if (a != b) parent[a] = b else redundant += 1 }
    return redundant
}
