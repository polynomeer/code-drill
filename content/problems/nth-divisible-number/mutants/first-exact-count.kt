// kind: OFF_BY_ONE
// 개수가 정확히 n 인 x 를 하나 찾으면 바로 돌려준다. 그런 x 는 여럿이고, 가장 작은 것이 답이다.
fun nthDivisible(n: Int, a: Int, b: Int, c: Int): Int {
    val cap = 2_000_000_000L
    fun gcd(x: Long, y: Long): Long = if (y == 0L) x else gcd(y, x % y)
    fun lcm(x: Long, y: Long): Long = minOf(x / gcd(x, y) * y, cap + 1)
    val ab = lcm(a.toLong(), b.toLong()); val bc = lcm(b.toLong(), c.toLong()); val ac = lcm(a.toLong(), c.toLong())
    val abc = lcm(ab, c.toLong())
    fun count(x: Long): Long = x / a + x / b + x / c - x / ab - x / bc - x / ac + x / abc
    var low = 1L; var high = cap
    while (low < high) {
        val mid = (low + high) / 2
        val k = count(mid)
        if (k == n.toLong()) return mid.toInt()
        if (k > n) high = mid else low = mid + 1
    }
    return low.toInt()
}
