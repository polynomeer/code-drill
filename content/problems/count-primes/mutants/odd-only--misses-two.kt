// kind: MISSING_EDGE_CASE
// 홀수만 소수 후보로 봐서 2 를 놓친다.
fun countPrimes(n: Int): Int {
    if (n < 3) return 0
    val composite = BooleanArray(n + 1)
    var count = 0
    var value = 3
    while (value <= n) {
        if (!composite[value]) {
            count += 1
            var multiple = value.toLong() * value
            while (multiple <= n) { composite[multiple.toInt()] = true; multiple += value }
        }
        value += 2
    }
    return count
}
