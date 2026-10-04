// 검증용 정답 (§6.1 solutions/). 위상 순서대로 색마다 최댓값을 옮긴다.
fun largestColorPath(colors: String, edges: IntArray): Int {
    val n = colors.length
    val m = edges.size / 2
    val head = IntArray(n) { -1 }
    val next = IntArray(m)
    val to = IntArray(m)
    val indegree = IntArray(n)
    for (e in 0 until m) {
        val a = edges[2 * e]; val b = edges[2 * e + 1]
        to[e] = b; next[e] = head[a]; head[a] = e
        indegree[b] += 1
    }
    val best = IntArray(n * 26)
    val queue = IntArray(n)
    var tail = 0
    for (v in 0 until n) if (indegree[v] == 0) { queue[tail] = v; tail += 1 }
    var front = 0
    var answer = 0
    while (front < tail) {
        val v = queue[front]
        front += 1
        Drill.dequeue(v)
        val own = v * 26 + (colors[v] - 'a')
        best[own] += 1
        Drill.write(v, best[own])
        answer = maxOf(answer, best[own])
        var e = head[v]
        while (e != -1) {
            val u = to[e]
            for (k in 0 until 26) {
                if (best[v * 26 + k] > best[u * 26 + k]) best[u * 26 + k] = best[v * 26 + k]
            }
            indegree[u] -= 1
            if (indegree[u] == 0) { queue[tail] = u; tail += 1 }
            e = next[e]
        }
    }
    return if (front == n) answer else -1
}
