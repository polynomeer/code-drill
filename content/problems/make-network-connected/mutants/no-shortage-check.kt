// kind: MISSING_EDGE_CASE
// 케이블이 모자라도 무리 수 − 1 을 답한다.
fun makeNetworkConnected(n: Int, cables: IntArray): Int {
    val parent = IntArray(n) { it }
    fun find(x: Int): Int { var r = x; while (parent[r] != r) { parent[r] = parent[parent[r]]; r = parent[r] }; return r }
    var components = n
    for (i in 0 until cables.size / 2) { val a = find(cables[2 * i]); val b = find(cables[2 * i + 1]); if (a != b) { parent[a] = b; components -= 1 } }
    return components - 1
}
