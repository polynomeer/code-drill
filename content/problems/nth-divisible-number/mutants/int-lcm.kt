// kind: MISSING_EDGE_CASE
// 최소공배수를 Int 로 계산한다. 두 수의 최소공배수가 2^31 을 넘으면 감겨 엉뚱한 작은 수가 되고, 그 몫이 빠진다.
fun nthDivisible(n: Int, a: Int, b: Int, c: Int): Int {
    fun gcd(x: Int, y: Int): Int = if (y == 0) x else gcd(y, x % y)
    fun lcm(x: Int, y: Int): Int = x / gcd(x, y) * y
    val ab = lcm(a, b); val bc = lcm(b, c); val ac = lcm(a, c)
    val abc = lcm(ab, c)
    fun count(x: Long): Long = x / a + x / b + x / c - x / ab - x / bc - x / ac + x / abc
    var low = 1L; var high = 2_000_000_000L
    while (low < high) { val mid = (low + high) / 2; if (count(mid) >= n) high = mid else low = mid + 1 }
    return low.toInt()
}
