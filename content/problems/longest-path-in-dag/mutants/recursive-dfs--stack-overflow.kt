// kind: WRONG_ALGORITHM
// 재귀 DFS 로 기억하며 푼다. 답은 맞지만 5만 개 사슬에서 스택이 넘친다.
fun longestPathInDag(n: Int, edges: IntArray): Int {
    val adj = Array(n) { ArrayList<Int>() }
    for (i in edges.indices step 2) adj[edges[i]].add(edges[i + 1])
    val memo = IntArray(n) { -1 }
    val state = IntArray(n)  // 0 미방문, 1 방문 중, 2 끝
    var cyclic = false
    fun go(u: Int): Int {
        if (state[u] == 1) { cyclic = true; return 0 }
        if (state[u] == 2) return memo[u]
        state[u] = 1
        var best = 0
        for (v in adj[u]) { Drill.compare(u, v); best = maxOf(best, go(v) + 1) }
        state[u] = 2; memo[u] = best
        return best
    }
    var answer = 0
    for (v in 0 until n) answer = maxOf(answer, go(v))
    return if (cyclic) -1 else answer
}
