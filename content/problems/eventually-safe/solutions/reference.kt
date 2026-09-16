// 검증용 정답 (§6.1 solutions/). 간선을 뒤집어 끝 정점에서부터 남은 나가는 간선을 센다.
fun safeNodes(n: Int, edges: IntArray): IntArray {
    val m = edges.size / 2
    val head = IntArray(n) { -1 }
    val next = IntArray(m)
    val to = IntArray(m)
    val outdegree = IntArray(n)
    var i = 0
    var e = 0
    while (i < edges.size) {
        val a = edges[i]; val b = edges[i + 1]
        to[e] = a; next[e] = head[b]; head[b] = e; e += 1
        outdegree[a] += 1
        i += 2
    }
    val queue = IntArray(n)
    var tail = 0
    for (v in 0 until n) if (outdegree[v] == 0) { queue[tail] = v; tail += 1; Drill.enqueue(v) }
    val safe = BooleanArray(n)
    var front = 0
    while (front < tail) {
        val v = queue[front]; front += 1
        Drill.dequeue(v)
        safe[v] = true
        var edge = head[v]
        while (edge != -1) {
            val prev = to[edge]
            Drill.edge("v$v", "v$prev")
            outdegree[prev] -= 1
            if (outdegree[prev] == 0) { queue[tail] = prev; tail += 1; Drill.enqueue(prev) }
            edge = next[edge]
        }
    }
    val out = IntArray(tail)
    var k = 0
    for (v in 0 until n) if (safe[v]) { out[k] = v; k += 1 }
    return out
}
