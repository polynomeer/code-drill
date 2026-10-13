// kind: WRONG_BRANCH
// 잎을 하나씩 벗기다 둘 이하가 남으면 멈춘다. 같은 겹의 잎은 동시에 떨어져야 한다 — 겹 가운데서 멈추면 가운데가 아닌 정점이 남는다.
fun minHeightRoots(n: Int, edges: IntArray): IntArray {
    if (n == 1) return intArrayOf(0)
    val degree = IntArray(n); val adj = Array(n) { ArrayList<Int>() }
    for (i in edges.indices step 2) { adj[edges[i]].add(edges[i + 1]); adj[edges[i + 1]].add(edges[i]); degree[edges[i]] += 1; degree[edges[i + 1]] += 1 }
    val queue = ArrayDeque<Int>()
    for (v in 0 until n) if (degree[v] == 1) queue.addLast(v)
    val removed = BooleanArray(n)
    var remaining = n
    while (remaining > 2) {
        val v = queue.removeFirst(); removed[v] = true; remaining -= 1
        for (u in adj[v]) if (!removed[u]) { degree[u] -= 1; if (degree[u] == 1) queue.addLast(u) }
    }
    return (0 until n).filter { !removed[it] }.toIntArray()
}
