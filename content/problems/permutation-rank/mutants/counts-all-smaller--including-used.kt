// kind: WRONG_ALGORITHM
// 자리마다 값 - 1 을 곱한다 — 이미 앞에 쓴 수까지 작은 수로 센다.
fun permutationRank(perm: IntArray): Int {
    val n = perm.size
    val factorial = IntArray(n + 1); factorial[0] = 1
    for (i in 1..n) factorial[i] = factorial[i - 1] * i
    var rank = 0
    for (i in 0 until n) rank += (perm[i] - 1) * factorial[n - 1 - i]
    return rank
}
