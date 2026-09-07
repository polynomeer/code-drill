// 검증용 정답 (§6.1 solutions/). 위상 정렬(칸 알고리즘).
//
// 들어오는 간선이 없는 정점부터 하나씩 확정해 나간다. 전부 확정하지 못하고 멈추면
// 남은 것들이 서로를 기다리고 있다는 뜻이고, 그것이 곧 순환이다.
//
// 모든 정점을 시작점으로 삼는 것이 중요하다. 0 에서만 탐색하면 거기서 닿을 수 없는
// 곳의 순환을 놓친다.
fun hasCycle(n: Int, edges: IntArray): Int {
    val outDegree = IntArray(n)
    val inDegree = IntArray(n)
    var i = 0
    while (i < edges.size) {
        outDegree[edges[i]] += 1
        inDegree[edges[i + 1]] += 1
        i += 2
    }

    val start = IntArray(n + 1)
    for (v in 0 until n) start[v + 1] = start[v] + outDegree[v]
    val cursor = start.copyOf()
    val flat = IntArray(edges.size / 2)
    i = 0
    while (i < edges.size) {
        flat[cursor[edges[i]]++] = edges[i + 1]
        i += 2
    }

    val queue = ArrayDeque<Int>()
    for (v in 0 until n) if (inDegree[v] == 0) queue.addLast(v)

    var done = 0
    while (queue.isNotEmpty()) {
        val node = queue.removeFirst()
        Drill.node("v$node")
        done += 1
        Drill.write(0, done)

        for (index in start[node] until start[node + 1]) {
            val next = flat[index]
            Drill.edge("v$node", "v$next")
            inDegree[next] -= 1
            if (inDegree[next] == 0) queue.addLast(next)
        }
    }
    return if (done == n) 0 else 1
}
