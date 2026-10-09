// kind: PERFORMANCE
// 간선 쌍마다 두 간선을 빼고 세 조각을 다시 훑는다. 맞지만, 간선 쌍 수 × 정점 수 — 2000 정점이면 수십억 걸음이다.
fun treeSplitXor(vals: IntArray, edges: IntArray): Int {
    val n = vals.size
    val m = edges.size / 2
    val adj = Array(n) { ArrayList<IntArray>() }
    for (e in 0 until m) { adj[edges[2 * e]].add(intArrayOf(edges[2 * e + 1], e)); adj[edges[2 * e + 1]].add(intArrayOf(edges[2 * e], e)) }
    var best = Int.MAX_VALUE
    val seen = IntArray(n) { -1 }
    var stamp = 0
    val stack = IntArray(n)
    for (e1 in 0 until m) for (e2 in e1 + 1 until m) {
        stamp += 1
        val parts = ArrayList<Int>()
        for (start in 0 until n) {
            if (seen[start] == stamp) continue
            var x = 0; var size = 0
            stack[size++] = start; seen[start] = stamp
            while (size > 0) {
                val v = stack[--size]; x = x xor vals[v]
                for (edge in adj[v]) if (edge[1] != e1 && edge[1] != e2 && seen[edge[0]] != stamp) { seen[edge[0]] = stamp; stack[size++] = edge[0] }
            }
            parts.add(x)
        }
        best = minOf(best, parts.maxOrNull()!! - parts.minOrNull()!!)
    }
    return best
}
