// 검증용 정답 (§6.1 solutions/). 자리마다 가능한 값만 시도하는 백트래킹.
fun beautifulArrangementCount(n: Int): Int {
    val used = BooleanArray(n + 1)
    var count = 0
    fun go(i: Int) {
        if (i > n) { count += 1; Drill.write(0, count); return }
        for (v in 1..n) {
            if (used[v] || (v % i != 0 && i % v != 0)) continue
            Drill.compare(i, v)
            used[v] = true
            go(i + 1)
            used[v] = false
        }
    }
    go(1)
    return count
}
