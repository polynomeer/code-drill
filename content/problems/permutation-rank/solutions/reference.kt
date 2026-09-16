// 검증용 정답 (§6.1 solutions/). 계승 진법 — 자리마다 뒤에 남은 더 작은 수를 센다.
fun permutationRank(perm: IntArray): Int {
    val n = perm.size
    val factorial = IntArray(n + 1)
    factorial[0] = 1
    for (i in 1..n) factorial[i] = factorial[i - 1] * i
    var rank = 0
    for (i in 0 until n) {
        var smaller = 0
        for (j in i + 1 until n) {
            Drill.compare(perm[i], perm[j])
            if (perm[j] < perm[i]) smaller += 1
        }
        rank += smaller * factorial[n - 1 - i]
        Drill.write(i, rank)
    }
    return rank
}
