// kind: PERFORMANCE
// 일마다 선행 일들을 재귀로 다시 계산한다. 마름모가 겹치면 지수적이다.
fun criticalPath(durations: IntArray, deps: IntArray): Int {
    val n = durations.size
    val before = Array(n) { ArrayList<Int>() }
    for (i in deps.indices step 2) before[deps[i + 1]].add(deps[i])
    val onPath = BooleanArray(n)
    var cyclic = false
    fun finish(v: Int): Int {
        if (onPath[v]) { cyclic = true; return 0 }
        onPath[v] = true
        var best = 0
        for (u in before[v]) { Drill.edge("t$u", "t$v"); best = maxOf(best, finish(u)) }
        onPath[v] = false
        return best + durations[v]
    }
    var best = 0
    for (v in 0 until n) { best = maxOf(best, finish(v)); if (cyclic) return -1 }
    return best
}
