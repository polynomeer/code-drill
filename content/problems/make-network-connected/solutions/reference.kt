// 검증용 정답 (§6.1 solutions/). 케이블이 모자라면 -1, 아니면 무리 수 − 1.
fun makeNetworkConnected(n: Int, cables: IntArray): Int {
    val m = cables.size / 2
    if (m < n - 1) return -1
    val parent = IntArray(n) { it }
    fun find(x: Int): Int { var r = x; while (parent[r] != r) { parent[r] = parent[parent[r]]; r = parent[r] }; return r }
    var components = n
    for (i in 0 until m) {
        val a = find(cables[2 * i]); val b = find(cables[2 * i + 1])
        Drill.compare(cables[2 * i], cables[2 * i + 1])
        if (a != b) { parent[a] = b; components -= 1; Drill.write(0, components) }
    }
    return components - 1
}
