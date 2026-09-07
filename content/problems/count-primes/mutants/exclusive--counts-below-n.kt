// kind: OFF_BY_ONE
// n 미만만 센다. n 자신이 소수일 때 하나 모자란다.
fun countPrimes(n: Int): Int {
    if (n < 2) return 0
    val composite = BooleanArray(n + 1)
    var count = 0
    for (value in 2 until n) {
        if (composite[value]) continue
        count += 1
        var multiple = value.toLong() * value
        while (multiple <= n) { composite[multiple.toInt()] = true; multiple += value }
    }
    return count
}
