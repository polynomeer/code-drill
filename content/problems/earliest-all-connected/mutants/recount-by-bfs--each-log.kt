// kind: PERFORMANCE
// 기록마다 그래프를 처음부터 훑어 무리 수를 다시 센다. O(m·(n+m)).
fun earliestAllConnected(n: Int, logs: IntArray): Int {
    if (n == 1) return 0
    val m = logs.size / 3
    val order = (0 until m).sortedBy { logs[it * 3] }
    val graph = Array(n) { mutableListOf<Int>() }
    for (k in order) {
        val a = logs[k * 3 + 1]; val b = logs[k * 3 + 2]
        graph[a].add(b); graph[b].add(a)
        val seen = BooleanArray(n)
        val queue = ArrayDeque<Int>()
        queue.addLast(0); seen[0] = true
        var count = 1
        while (queue.isNotEmpty()) {
            val v = queue.removeFirst()
            for (nxt in graph[v]) { Drill.compare(v, nxt); if (!seen[nxt]) { seen[nxt] = true; count += 1; queue.addLast(nxt) } }
        }
        if (count == n) return logs[k * 3]
    }
    return -1
}
