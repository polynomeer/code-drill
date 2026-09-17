// kind: OFF_BY_ONE
// 자리를 0 부터 센다. 0 으로 나누는 자리를 피하려 조건이 어긋난다.
fun beautifulArrangementCount(n: Int): Int {
    val used = BooleanArray(n + 1)
    var count = 0
    fun go(i: Int) {
        if (i >= n) { count += 1; return }
        for (v in 1..n) {
            if (used[v] || (i != 0 && v % i != 0 && i % v != 0)) continue
            used[v] = true; go(i + 1); used[v] = false
        }
    }
    go(0)
    return count
}
