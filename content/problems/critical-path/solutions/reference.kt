// 검증용 정답 (§6.1 solutions/). 칸 알고리즘으로 위상 순서를 돌며 끝나는 시각을 민다.
fun criticalPath(durations: IntArray, deps: IntArray): Int {
    val n = durations.size
    val after = Array(n) { ArrayList<Int>() }
    val indeg = IntArray(n)
    for (i in deps.indices step 2) { after[deps[i]].add(deps[i + 1]); indeg[deps[i + 1]] += 1 }
    val finish = IntArray(n)
    val ready = ArrayDeque<Int>()
    for (v in 0 until n) if (indeg[v] == 0) ready.addLast(v)
    var seen = 0
    var best = 0
    while (ready.isNotEmpty()) {
        val v = ready.removeLast()
        seen += 1
        finish[v] += durations[v]
        Drill.node("t$v")
        Drill.write(v, finish[v])
        best = maxOf(best, finish[v])
        for (u in after[v]) {
            Drill.edge("t$v", "t$u")
            finish[u] = maxOf(finish[u], finish[v])
            indeg[u] -= 1
            if (indeg[u] == 0) ready.addLast(u)
        }
    }
    return if (seen == n) best else -1
}
