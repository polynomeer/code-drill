// kind: PERFORMANCE
// 시작마다 끝을 늘려 가며 최솟값과 합을 갱신한다. 맞지만, 10 만 명이면 50 억 구간이다.
fun wizardStrengthSum(strength: IntArray): Int {
    val mod = 1_000_000_007L
    var total = 0L
    for (start in strength.indices) {
        var low = Int.MAX_VALUE
        var sum = 0L
        for (end in start until strength.size) {
            low = minOf(low, strength[end])
            sum = (sum + strength[end]) % mod
            total = (total + low % mod * sum) % mod
        }
    }
    return total.toInt()
}
