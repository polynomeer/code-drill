// kind: WRONG_ALGORITHM
// 방문 표시를 정점만으로 한다. 한 색으로 먼저 온 정점을 다른 색으로 다시 지나지 못한다.
fun alternatingColorPaths(n: Int, red: IntArray, blue: IntArray): IntArray {
    val adj = Array(2) { Array(n) { ArrayList<Int>() } }
    for (i in red.indices step 2) adj[0][red[i]].add(red[i + 1])
    for (i in blue.indices step 2) adj[1][blue[i]].add(blue[i + 1])
    val dist = IntArray(n) { -1 }
    dist[0] = 0
    val queue = ArrayDeque<Int>()
    queue.addLast(0); queue.addLast(1)
    while (queue.isNotEmpty()) {
        val state = queue.removeFirst()
        val u = state / 2; val next = 1 - state % 2
        for (v in adj[next][u]) if (dist[v] < 0) { dist[v] = dist[u] + 1; queue.addLast(v * 2 + next) }
    }
    return dist
}
