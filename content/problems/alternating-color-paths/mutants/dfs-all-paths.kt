// kind: PERFORMANCE
// 번갈아 가는 모든 경로를 DFS 로 열거하며 최솟값을 갱신한다. 지수다.
fun alternatingColorPaths(n: Int, red: IntArray, blue: IntArray): IntArray {
    val adj = Array(2) { Array(n) { ArrayList<Int>() } }
    for (i in red.indices step 2) adj[0][red[i]].add(red[i + 1])
    for (i in blue.indices step 2) adj[1][blue[i]].add(blue[i + 1])
    val best = IntArray(n) { -1 }
    best[0] = 0
    val onPath = BooleanArray(n)
    fun go(u: Int, last: Int, depth: Int) {
        for (v in adj[1 - last][u]) {
            Drill.compare(u, v)
            if (best[v] < 0 || depth + 1 < best[v]) best[v] = depth + 1
            if (!onPath[v]) { onPath[v] = true; go(v, 1 - last, depth + 1); onPath[v] = false }
        }
    }
    onPath[0] = true
    go(0, 0, 0); go(0, 1, 0)
    return best
}
