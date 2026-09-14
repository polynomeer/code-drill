// 검증용 정답 (§6.1 solutions/). Kahn 알고리즘을 층 단위로.
fun minSemesters(n: Int, prereqs: IntArray): Int {
    val indegree = IntArray(n)
    val head = IntArray(n) { -1 }
    val next = IntArray(prereqs.size / 2)
    val to = IntArray(prereqs.size / 2)
    var i = 0
    var e = 0
    while (i < prereqs.size) {
        val before = prereqs[i]
        val after = prereqs[i + 1]
        to[e] = after
        next[e] = head[before]
        head[before] = e
        indegree[after] += 1
        i += 2
        e += 1
    }
    val queue = ArrayDeque<Int>()
    for (v in 0 until n) if (indegree[v] == 0) { queue.addLast(v); Drill.enqueue(v) }
    var taken = 0
    var semesters = 0
    while (queue.isNotEmpty()) {
        semesters += 1
        repeat(queue.size) {
            val node = queue.removeFirst()
            Drill.node("c$node")
            taken += 1
            var edge = head[node]
            while (edge != -1) {
                val after = to[edge]
                Drill.edge("c$node", "c$after")
                indegree[after] -= 1
                if (indegree[after] == 0) { queue.addLast(after); Drill.enqueue(after) }
                edge = next[edge]
            }
        }
        Drill.write(semesters, taken)
    }
    return if (taken == n) semesters else -1
}
