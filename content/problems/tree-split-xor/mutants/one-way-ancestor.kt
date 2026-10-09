// kind: WRONG_BRANCH
// b 가 a 의 자손인지만 본다. 번호가 큰 쪽이 조상일 수도 있다.
fun treeSplitXor(vals: IntArray, edges: IntArray): Int {
    val n = vals.size
    val adj = Array(n) { ArrayList<Int>() }
    for (i in edges.indices step 2) { adj[edges[i]].add(edges[i + 1]); adj[edges[i + 1]].add(edges[i]) }
    val tin = IntArray(n); val tout = IntArray(n); val sub = vals.copyOf(); val parent = IntArray(n) { -1 }
    var clock = 0
    // 재귀 대신 스택: 음수는 "자식을 다 본 뒤"의 표시다.
    val stack = ArrayDeque<Int>()
    stack.addLast(0)
    while (stack.isNotEmpty()) {
        val top = stack.removeLast()
        if (top >= 0) {
            tin[top] = clock; clock += 1
            stack.addLast(-top - 1)
            for (u in adj[top]) if (u != parent[top]) { parent[u] = top; stack.addLast(u) }
        } else {
            val v = -top - 1
            tout[v] = clock
            if (parent[v] >= 0) sub[parent[v]] = sub[parent[v]] xor sub[v]
            Drill.write(v, sub[v])
        }
    }
    val total = sub[0]
    var best = Int.MAX_VALUE
    for (a in 1 until n) for (b in a + 1 until n) {
        val x: Int; val y: Int; val z: Int
        if (tin[b] >= tin[a] && tin[b] < tout[a]) { x = sub[b]; y = sub[a] xor sub[b]; z = total xor sub[a] }
        else { x = sub[a]; y = sub[b]; z = total xor sub[a] xor sub[b] }
        val score = maxOf(x, maxOf(y, z)) - minOf(x, minOf(y, z))
        if (score < best) best = score
    }
    return best
}
