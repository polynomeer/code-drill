// kind: WRONG_ALGORITHM
// 번호 순서가 위상 순서라고 믿고 0 부터 차례로 민다.
fun criticalPath(durations: IntArray, deps: IntArray): Int {
    val n = durations.size
    val after = Array(n) { ArrayList<Int>() }
    for (i in deps.indices step 2) { if (deps[i] == deps[i + 1]) return -1; after[deps[i]].add(deps[i + 1]) }
    val finish = IntArray(n)
    var best = 0
    for (v in 0 until n) {
        finish[v] += durations[v]; best = maxOf(best, finish[v])
        for (u in after[v]) finish[u] = maxOf(finish[u], finish[v])
    }
    return best
}
