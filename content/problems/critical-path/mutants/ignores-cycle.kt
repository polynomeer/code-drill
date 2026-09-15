// kind: MISSING_EDGE_CASE
// 순환이 있어 시작하지 못한 일을 그냥 두고 지금까지의 최댓값을 답한다.
fun criticalPath(durations: IntArray, deps: IntArray): Int {
    val n = durations.size
    val after = Array(n) { ArrayList<Int>() }
    val indeg = IntArray(n)
    for (i in deps.indices step 2) { after[deps[i]].add(deps[i + 1]); indeg[deps[i + 1]] += 1 }
    val finish = IntArray(n)
    val ready = ArrayDeque<Int>()
    for (v in 0 until n) if (indeg[v] == 0) ready.addLast(v)
    var best = 0
    while (ready.isNotEmpty()) {
        val v = ready.removeLast()
        finish[v] += durations[v]; best = maxOf(best, finish[v])
        for (u in after[v]) { finish[u] = maxOf(finish[u], finish[v]); indeg[u] -= 1; if (indeg[u] == 0) ready.addLast(u) }
    }
    return best
}
