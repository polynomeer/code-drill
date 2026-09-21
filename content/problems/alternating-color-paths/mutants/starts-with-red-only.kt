// kind: MISSING_EDGE_CASE
// 첫 간선을 빨강으로만 시작한다. 파랑으로 시작해야 닿는 정점을 놓친다.
fun alternatingColorPaths(n: Int, red: IntArray, blue: IntArray): IntArray {
    val adj = Array(2) { Array(n) { ArrayList<Int>() } }
    for (i in red.indices step 2) adj[0][red[i]].add(red[i + 1])
    for (i in blue.indices step 2) adj[1][blue[i]].add(blue[i + 1])
    val dist = Array(2) { IntArray(n) { -1 } }
    dist[1][0] = 0
    val queue = ArrayDeque<Int>()
    queue.addLast(1)
    while (queue.isNotEmpty()) {
        val state = queue.removeFirst()
        val u = state / 2; val last = state % 2; val next = 1 - last
        for (v in adj[next][u]) if (dist[next][v] < 0) { dist[next][v] = dist[last][u] + 1; queue.addLast(v * 2 + next) }
    }
    return IntArray(n) { v -> val a = dist[0][v]; val b = dist[1][v]; if (a < 0) b else if (b < 0) a else minOf(a, b) }
}
