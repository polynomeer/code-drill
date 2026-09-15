// kind: PERFORMANCE
// 간선을 고를 때마다 지금까지 고른 간선으로 DFS 해 이미 이어졌는지 본다. O(E · V).
fun minSpanningCost(n: Int, edges: IntArray): Int {
    val m = edges.size / 3
    val order = (0 until m).sortedBy { edges[3 * it + 2] }
    val adj = Array(n) { mutableListOf<Int>() }
    val seen = BooleanArray(n)
    fun connected(a: Int, b: Int): Boolean {
        java.util.Arrays.fill(seen, false)
        val stack = ArrayDeque<Int>(); stack.addLast(a); seen[a] = true
        while (stack.isNotEmpty()) {
            val v = stack.removeLast()
            Drill.visit(v, 0)
            if (v == b) return true
            for (u in adj[v]) if (!seen[u]) { seen[u] = true; stack.addLast(u) }
        }
        return false
    }
    var total = 0
    var joined = 0
    for (i in order) {
        if (joined == n - 1) break
        val a = edges[3 * i]; val b = edges[3 * i + 1]
        if (connected(a, b)) continue
        adj[a].add(b); adj[b].add(a)
        total += edges[3 * i + 2]
        joined += 1
    }
    return if (joined == n - 1) total else -1
}
