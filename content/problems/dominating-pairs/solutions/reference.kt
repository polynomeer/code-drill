// 검증용 정답 (§6.1 solutions/). CDQ — 등번호로 반을 가르고, 두 반을 가로지르는 쌍은 y 로 정렬해 z 의 펜윅 트리로 센다.
fun dominatingPairs(ys: IntArray, zs: IntArray): Int {
    val n = ys.size
    val tree = IntArray(n + 1)
    fun add(z: Int, d: Int) { var i = z + 1; while (i <= n) { tree[i] += d; i += i and -i } }
    fun countBelow(z: Int): Int { var i = z; var s = 0; while (i > 0) { s += tree[i]; i -= i and -i }; return s }
    // order[low..high) 는 재귀가 끝나면 y 순으로 정렬돼 있다(병합 정렬처럼)
    val order = IntArray(n) { it }
    val buffer = IntArray(n)
    var total = 0L
    fun solve(low: Int, high: Int) {
        if (high - low <= 1) return
        val mid = (low + high) ushr 1
        solve(low, mid); solve(mid, high)
        var p = low
        for (k in mid until high) {
            val j = order[k]
            while (p < mid && ys[order[p]] < ys[j]) { add(zs[order[p]], 1); p += 1 }
            total += countBelow(zs[j])
        }
        for (k in low until p) add(zs[order[k]], -1)
        // y 로 병합한다
        var a = low; var b = mid; var c = low
        while (a < mid || b < high) buffer[c++] = if (b >= high || (a < mid && ys[order[a]] < ys[order[b]])) order[a++] else order[b++]
        for (k in low until high) order[k] = buffer[k]
    }
    solve(0, n)
    Drill.ret("0..$n", (total % 1_000_000_007).toInt())
    return (total % 1_000_000_007).toInt()
}
