// kind: WRONG_BRANCH
// DFS 에서 방문 중인 정점을 표시하지 않아 순환에 걸린 정점을 안전하다고 적는다.
fun safeNodes(n: Int, edges: IntArray): IntArray {
    val graph = Array(n) { mutableListOf<Int>() }
    var i = 0
    while (i < edges.size) { graph[edges[i]].add(edges[i + 1]); i += 2 }
    val memo = IntArray(n) // 0 모름, 1 안전, 2 위험
    fun safe(v: Int): Boolean {
        if (memo[v] != 0) return memo[v] == 1
        memo[v] = 1
        for (nxt in graph[v]) if (!safe(nxt)) { memo[v] = 2; return false }
        return true
    }
    return (0 until n).filter { safe(it) }.toIntArray()
}
