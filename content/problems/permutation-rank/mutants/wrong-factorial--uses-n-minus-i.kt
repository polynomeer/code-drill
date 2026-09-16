// kind: OFF_BY_ONE
// 자리 i 의 가중치를 (n-i)! 로 둔다. 한 자리씩 큰 계승을 쓴다.
fun permutationRank(perm: IntArray): Int {
    val n = perm.size
    val factorial = IntArray(n + 1); factorial[0] = 1
    for (i in 1..n) factorial[i] = factorial[i - 1] * i
    var rank = 0
    for (i in 0 until n) {
        var smaller = 0
        for (j in i + 1 until n) if (perm[j] < perm[i]) smaller += 1
        rank += smaller * factorial[n - i]
    }
    return rank
}
