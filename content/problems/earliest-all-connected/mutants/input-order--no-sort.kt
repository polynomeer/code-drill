// kind: WRONG_ALGORITHM
// 기록을 주어진 순서대로 반영한다. 시각순이 아니면 답이 나중 기록의 시각이 된다.
fun earliestAllConnected(n: Int, logs: IntArray): Int {
    if (n == 1) return 0
    val parent = IntArray(n) { it }
    fun find(x: Int): Int { var v = x; while (parent[v] != v) v = parent[v]; return v }
    var groups = n
    var i = 0
    while (i < logs.size) {
        val ra = find(logs[i + 1]); val rb = find(logs[i + 2])
        if (ra != rb) { parent[ra] = rb; groups -= 1; if (groups == 1) return logs[i] }
        i += 3
    }
    return -1
}
