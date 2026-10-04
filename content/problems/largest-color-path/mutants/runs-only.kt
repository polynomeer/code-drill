// kind: WRONG_ALGORITHM
// 같은 색이 연달아 나올 때만 센다. 사이에 다른 색이 끼면 그때까지 센 개수를 잃는다.
fun largestColorPath(colors: String, edges: IntArray): Int {
    val n = colors.length
    val adj = Array(n) { ArrayList<Int>() }
    val indegree = IntArray(n)
    for (i in edges.indices step 2) { adj[edges[i]].add(edges[i + 1]); indegree[edges[i + 1]] += 1 }
    val run = IntArray(n) { 1 }
    val queue = java.util.ArrayDeque<Int>()
    for (v in 0 until n) if (indegree[v] == 0) queue.add(v)
    var seen = 0
    var answer = 0
    while (queue.isNotEmpty()) {
        val v = queue.poll()
        seen += 1
        answer = maxOf(answer, run[v])
        for (u in adj[v]) {
            if (colors[u] == colors[v]) run[u] = maxOf(run[u], run[v] + 1)
            indegree[u] -= 1
            if (indegree[u] == 0) queue.add(u)
        }
    }
    return if (seen == n) answer else -1
}
