// kind: OFF_BY_ONE
// 케이블이 n 개 미만이면 -1 이라고 한다. n-1 개면 충분하다.
fun makeNetworkConnected(n: Int, cables: IntArray): Int {
    val m = cables.size / 2
    if (m < n) return -1
    val parent = IntArray(n) { it }
    fun find(x: Int): Int { var r = x; while (parent[r] != r) { parent[r] = parent[parent[r]]; r = parent[r] }; return r }
    var components = n
    for (i in 0 until m) { val a = find(cables[2 * i]); val b = find(cables[2 * i + 1]); if (a != b) { parent[a] = b; components -= 1 } }
    return components - 1
}
