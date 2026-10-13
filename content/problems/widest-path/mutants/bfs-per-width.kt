// kind: PERFORMANCE
// 서로 다른 폭마다 그 폭 이상의 간선만으로 BFS 를 다시 한다. 맞지만, 간선 × 서로 다른 폭이다.
fun widestPath(n: Int, edges: IntArray, s: Int, t: Int): Int {
    val m = edges.size / 3
    val widths = (0 until m).map { edges[3 * it + 2] }.distinct().sortedDescending()
    val adj = Array(n) { ArrayList<IntArray>() }
    for (e in 0 until m) { adj[edges[3 * e]].add(intArrayOf(edges[3 * e + 1], edges[3 * e + 2])); adj[edges[3 * e + 1]].add(intArrayOf(edges[3 * e], edges[3 * e + 2])) }
    val seen = IntArray(n) { -1 }
    val queue = IntArray(n)
    for ((round, w) in widths.withIndex()) {
        var head = 0; var tail = 0
        queue[tail++] = s; seen[s] = round
        while (head < tail) {
            val u = queue[head++]
            for (edge in adj[u]) if (edge[1] >= w && seen[edge[0]] != round) { seen[edge[0]] = round; queue[tail++] = edge[0] }
        }
        if (seen[t] == round) return w
    }
    return -1
}
