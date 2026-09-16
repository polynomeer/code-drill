// 검증용 정답 (§6.1 solutions/). 남은 선수가 없는 과목 중 가장 작은 것을 힙으로 꺼낸다.
fun smallestOrder(n: Int, prereqs: IntArray): IntArray {
    val m = prereqs.size / 2
    val head = IntArray(n) { -1 }
    val next = IntArray(m)
    val to = IntArray(m)
    val indegree = IntArray(n)
    var i = 0
    var e = 0
    while (i < prereqs.size) {
        to[e] = prereqs[i + 1]; next[e] = head[prereqs[i]]; head[prereqs[i]] = e; e += 1
        indegree[prereqs[i + 1]] += 1
        i += 2
    }
    val heap = java.util.PriorityQueue<Int>()
    for (v in 0 until n) if (indegree[v] == 0) { heap.add(v); Drill.enqueue(v) }
    val order = IntArray(n)
    var taken = 0
    while (heap.isNotEmpty()) {
        val v = heap.poll()
        Drill.dequeue(v)
        order[taken] = v
        taken += 1
        var edge = head[v]
        while (edge != -1) {
            val nxt = to[edge]
            Drill.edge("c$v", "c$nxt")
            indegree[nxt] -= 1
            if (indegree[nxt] == 0) { heap.add(nxt); Drill.enqueue(nxt) }
            edge = next[edge]
        }
    }
    return if (taken == n) order else IntArray(0)
}
