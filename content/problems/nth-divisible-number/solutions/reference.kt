// 검증용 정답 (§6.1 solutions/). x 이하의 개수를 포함·배제로 세고, 처음 n 이상이 되는 x 를 이분 탐색한다.
fun nthDivisible(n: Int, a: Int, b: Int, c: Int): Int {
    val cap = 2_000_000_000L
    fun gcd(x: Long, y: Long): Long = if (y == 0L) x else gcd(y, x % y)
    // 상한을 넘는 최소공배수는 상한 + 1 로 자른다 — 그 몫은 언제나 0 이고 Long 도 넘치지 않는다.
    fun lcm(x: Long, y: Long): Long = minOf(x / gcd(x, y) * y, cap + 1)
    val ab = lcm(a.toLong(), b.toLong()); val bc = lcm(b.toLong(), c.toLong()); val ac = lcm(a.toLong(), c.toLong())
    val abc = lcm(ab, c.toLong())
    fun count(x: Long): Long = x / a + x / b + x / c - x / ab - x / bc - x / ac + x / abc
    var low = 1L
    var high = cap
    while (low < high) {
        val mid = (low + high) / 2
        if (count(mid) >= n) high = mid else low = mid + 1
        Drill.pointer("low", low.toInt()); Drill.pointer("high", high.toInt())
    }
    return low.toInt()
}
