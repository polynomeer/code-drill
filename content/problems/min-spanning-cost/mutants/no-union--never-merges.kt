// kind: WRONG_BRANCH
// 두 정점이 같은 묶음인지 보지만 합치지는 않는다. 자기 자신을 잇는 간선 말고는 전부 고른다.
fun minSpanningCost(n: Int, edges: IntArray): Int {
    val m = edges.size / 3
    val order = (0 until m).sortedBy { edges[3 * it + 2] }
    val parent = IntArray(n) { it }
    var total = 0
    var joined = 0
    for (i in order) {
        if (joined == n - 1) break
        if (parent[edges[3 * i]] == parent[edges[3 * i + 1]] && edges[3 * i] == edges[3 * i + 1]) continue
        total += edges[3 * i + 2]
        joined += 1
    }
    return if (joined == n - 1) total else -1
}
