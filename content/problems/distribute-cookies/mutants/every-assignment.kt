// kind: PERFORMANCE
// 봉지마다 아이를 고르는 모든 경우를 본다. 맞지만, k^n 이다.
fun distributeCookies(bags: IntArray, k: Int): Int {
    val kids = IntArray(k)
    var best = Int.MAX_VALUE
    fun go(i: Int) {
        if (i == bags.size) { best = minOf(best, kids.maxOrNull()!!); return }
        for (c in 0 until k) { kids[c] += bags[i]; go(i + 1); kids[c] -= bags[i] }
    }
    go(0)
    return best
}
