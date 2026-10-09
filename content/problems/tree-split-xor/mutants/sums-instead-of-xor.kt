// kind: WRONG_ALGORITHM
// 조각의 값을 XOR 대신 합으로 낸다.
fun treeSplitXor(vals: IntArray, edges: IntArray): Int {
    val n = vals.size
    val adj = Array(n) { ArrayList<Int>() }
    for (i in edges.indices step 2) { adj[edges[i]].add(edges[i + 1]); adj[edges[i + 1]].add(edges[i]) }
    val tin = IntArray(n); val tout = IntArray(n); val sub = LongArray(n) { vals[it].toLong() }; val parent = IntArray(n) { -1 }
    var clock = 0
    val stack = ArrayDeque<Int>(); stack.addLast(0)
    while (stack.isNotEmpty()) {
        val top = stack.removeLast()
        if (top >= 0) { tin[top] = clock; clock += 1; stack.addLast(-top - 1); for (u in adj[top]) if (u != parent[top]) { parent[u] = top; stack.addLast(u) } }
        else { val v = -top - 1; tout[v] = clock; if (parent[v] >= 0) sub[parent[v]] += sub[v] }
    }
    val total = sub[0]
    var best = Long.MAX_VALUE
    for (a in 1 until n) for (b in a + 1 until n) {
        val x: Long; val y: Long; val z: Long
        if (tin[b] >= tin[a] && tin[b] < tout[a]) { x = sub[b]; y = sub[a] - sub[b]; z = total - sub[a] }
        else if (tin[a] >= tin[b] && tin[a] < tout[b]) { x = sub[a]; y = sub[b] - sub[a]; z = total - sub[b] }
        else { x = sub[a]; y = sub[b]; z = total - sub[a] - sub[b] }
        best = minOf(best, maxOf(x, maxOf(y, z)) - minOf(x, minOf(y, z)))
    }
    return best.toInt()
}
