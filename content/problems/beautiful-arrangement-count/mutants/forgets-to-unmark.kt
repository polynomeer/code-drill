// kind: MISSING_EDGE_CASE
// 되돌아올 때 사용 표시를 지우지 않는다. 첫 가지 뒤의 배치가 전부 사라진다.
fun beautifulArrangementCount(n: Int): Int {
    val used = BooleanArray(n + 1)
    var count = 0
    fun go(i: Int) {
        if (i > n) { count += 1; return }
        for (v in 1..n) {
            if (used[v] || (v % i != 0 && i % v != 0)) continue
            used[v] = true; go(i + 1)
        }
    }
    go(1)
    return count
}
