// kind: WRONG_ALGORITHM
// 셋의 최소공배수로 나눈 몫을 다시 더하지 않는다. 셋 모두의 배수가 한 번도 세어지지 않는다.
fun nthDivisible(n: Int, a: Int, b: Int, c: Int): Int {
    val cap = 2_000_000_000L
    fun gcd(x: Long, y: Long): Long = if (y == 0L) x else gcd(y, x % y)
    fun lcm(x: Long, y: Long): Long = minOf(x / gcd(x, y) * y, cap + 1)
    val ab = lcm(a.toLong(), b.toLong()); val bc = lcm(b.toLong(), c.toLong()); val ac = lcm(a.toLong(), c.toLong())
    fun count(x: Long): Long = x / a + x / b + x / c - x / ab - x / bc - x / ac
    var low = 1L; var high = cap
    while (low < high) { val mid = (low + high) / 2; if (count(mid) >= n) high = mid else low = mid + 1 }
    return low.toInt()
}
