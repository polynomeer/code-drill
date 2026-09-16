// kind: PERFORMANCE
// 정점마다 처음부터 DFS 를 돌려 순환에 닿는지 본다. 사슬에서 O(n²).
fun safeNodes(n: Int, edges: IntArray): IntArray {
    val graph = Array(n) { mutableListOf<Int>() }
    var i = 0
    while (i < edges.size) { graph[edges[i]].add(edges[i + 1]); i += 2 }
    fun reachesCycle(start: Int): Boolean {
        val state = IntArray(n)
        val stack = ArrayDeque<IntArray>()
        stack.addLast(intArrayOf(start, 0)); state[start] = 1
        while (stack.isNotEmpty()) {
            val top = stack.last()
            val v = top[0]
            if (top[1] < graph[v].size) {
                val nxt = graph[v][top[1]]; top[1] += 1
                Drill.compare(v, nxt)
                if (state[nxt] == 1) return true
                if (state[nxt] == 0) { state[nxt] = 1; stack.addLast(intArrayOf(nxt, 0)) }
            } else { state[v] = 2; stack.removeLast() }
        }
        return false
    }
    return (0 until n).filter { !reachesCycle(it) }.toIntArray()
}
