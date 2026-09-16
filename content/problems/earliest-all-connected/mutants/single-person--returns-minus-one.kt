// kind: MISSING_EDGE_CASE
// 사람이 한 명일 때 -1 을 돌려준다. 혼자면 이미 모두가 아는 사이다.
fun earliestAllConnected(n: Int, logs: IntArray): Int {
    val m = logs.size / 3
    val order = (0 until m).sortedBy { logs[it * 3] }
    val parent = IntArray(n) { it }
    fun find(x: Int): Int { var v = x; while (parent[v] != v) { parent[v] = parent[parent[v]]; v = parent[v] }; return v }
    var groups = n
    for (k in order) {
        val ra = find(logs[k * 3 + 1]); val rb = find(logs[k * 3 + 2])
        if (ra != rb) { parent[ra] = rb; groups -= 1; if (groups == 1) return logs[k * 3] }
    }
    return -1
}
