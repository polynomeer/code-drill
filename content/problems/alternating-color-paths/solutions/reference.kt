// 검증용 정답 (§6.1 solutions/). (정점, 마지막 색) 상태의 BFS, 0 은 두 상태로 시작.
fun alternatingColorPaths(n: Int, red: IntArray, blue: IntArray): IntArray {
    val adj = Array(2) { Array(n) { ArrayList<Int>() } }
    for (i in red.indices step 2) adj[0][red[i]].add(red[i + 1])
    for (i in blue.indices step 2) adj[1][blue[i]].add(blue[i + 1])
    val dist = Array(2) { IntArray(n) { -1 } }
    dist[0][0] = 0; dist[1][0] = 0
    val queue = ArrayDeque<Int>()
    queue.addLast(0); queue.addLast(1)          // 상태 = 정점 * 2 + 마지막 색
    while (queue.isNotEmpty()) {
        val state = queue.removeFirst()
        val u = state / 2; val last = state % 2; val next = 1 - last
        for (v in adj[next][u]) {
            Drill.compare(u, v)
            if (dist[next][v] < 0) { dist[next][v] = dist[last][u] + 1; Drill.write(v, dist[next][v]); queue.addLast(v * 2 + next) }
        }
    }
    return IntArray(n) { v ->
        val a = dist[0][v]; val b = dist[1][v]
        if (a < 0) b else if (b < 0) a else minOf(a, b)
    }
}
