// kind: WRONG_BRANCH
// perm[i] % i == 0 만 본다. i % perm[i] == 0 인 배치를 놓친다.
fun beautifulArrangementCount(n: Int): Int {
    val used = BooleanArray(n + 1)
    var count = 0
    fun go(i: Int) {
        if (i > n) { count += 1; return }
        for (v in 1..n) {
            if (used[v] || v % i != 0) continue
            used[v] = true; go(i + 1); used[v] = false
        }
    }
    go(1)
    return count
}
