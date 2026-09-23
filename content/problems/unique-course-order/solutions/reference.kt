// 검증용 정답 (§6.1 solutions/). Kahn 을 돌리며 큐에 둘 이상이 들어오는 순간 갈라진다.
fun uniqueCourseOrder(n: Int, edges: IntArray): Int {
    val head = IntArray(n) { -1 }
    val next = IntArray(edges.size / 2)
    val to = IntArray(edges.size / 2)
    val indegree = IntArray(n)
    for (i in edges.indices step 2) {
        val e = i / 2
        to[e] = edges[i + 1]; next[e] = head[edges[i]]; head[edges[i]] = e
        indegree[edges[i + 1]] += 1
    }
    val queue = IntArray(n); var front = 0; var back = 0
    for (v in 0 until n) if (indegree[v] == 0) { queue[back++] = v; Drill.enqueue(v) }
    var seen = 0
    while (front < back) {
        if (back - front > 1) return 0
        val u = queue[front++]
        Drill.dequeue(u)
        seen += 1
        var e = head[u]
        while (e != -1) {
            val v = to[e]
            indegree[v] -= 1
            if (indegree[v] == 0) { queue[back++] = v; Drill.enqueue(v) }
            e = next[e]
        }
    }
    return if (seen == n) 1 else 0
}
