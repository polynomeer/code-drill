// kind: WRONG_BRANCH
// 봉지를 정렬해 연속한 덩어리로만 나눈다. 아이가 받는 봉지는 연속할 필요가 없다.
fun distributeCookies(bags: IntArray, k: Int): Int {
    val sorted = bags.sorted()
    val n = sorted.size
    val prefix = IntArray(n + 1); for (i in 0 until n) prefix[i + 1] = prefix[i] + sorted[i]
    // best[j][i]: 앞 i 개를 j 명에게 연속으로 나눈 가장 작은 최댓값
    val inf = Int.MAX_VALUE
    var best = IntArray(n + 1) { prefix[it] }
    repeat(k - 1) {
        val next = IntArray(n + 1) { inf }
        for (i in 0..n) for (cut in 0..i) next[i] = minOf(next[i], maxOf(best[cut], prefix[i] - prefix[cut]))
        best = next
    }
    return best[n]
}
