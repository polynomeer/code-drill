// kind: OFF_BY_ONE
// 1 부터 센다.
fun permutationRank(perm: IntArray): Int {
    val n = perm.size
    val factorial = IntArray(n + 1); factorial[0] = 1
    for (i in 1..n) factorial[i] = factorial[i - 1] * i
    var rank = 1
    for (i in 0 until n) {
        var smaller = 0
        for (j in i + 1 until n) if (perm[j] < perm[i]) smaller += 1
        rank += smaller * factorial[n - 1 - i]
    }
    return rank
}
