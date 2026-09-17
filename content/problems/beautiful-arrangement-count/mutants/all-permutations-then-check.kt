// kind: PERFORMANCE
// 순열 전부를 만든 뒤 검사한다. 15! 이다.
fun beautifulArrangementCount(n: Int): Int {
    val perm = IntArray(n + 1)
    val used = BooleanArray(n + 1)
    var count = 0
    fun go(i: Int) {
        if (i > n) {
            var ok = true
            for (p in 1..n) if (perm[p] % p != 0 && p % perm[p] != 0) { ok = false; break }
            if (ok) count += 1
            return
        }
        for (v in 1..n) {
            if (used[v]) continue
            Drill.compare(i, v)
            used[v] = true; perm[i] = v; go(i + 1); used[v] = false
        }
    }
    go(1)
    return count
}
