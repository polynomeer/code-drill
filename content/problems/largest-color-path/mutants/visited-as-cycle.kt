// kind: WRONG_BRANCH
// 깊이 우선 탐색에서 한 번 본 정점을 다시 만나면 순환이라고 본다. 두 갈래가 다시 만나는 것은 순환이 아니다.
fun largestColorPath(colors: String, edges: IntArray): Int {
    val n = colors.length
    val adj = Array(n) { ArrayList<Int>() }
    for (i in edges.indices step 2) adj[edges[i]].add(edges[i + 1])
    val seen = BooleanArray(n)
    val best = Array(n) { IntArray(26) }
    var cycle = false
    fun dfs(v: Int) {
        seen[v] = true
        for (u in adj[v]) {
            if (seen[u]) { cycle = true; continue }
            dfs(u)
            for (k in 0 until 26) best[v][k] = maxOf(best[v][k], best[u][k])
        }
        best[v][colors[v] - 'a'] += 1
    }
    for (v in 0 until n) if (!seen[v]) dfs(v)
    if (cycle) return -1
    var answer = 0
    for (v in 0 until n) for (k in 0 until 26) answer = maxOf(answer, best[v][k])
    return answer
}
