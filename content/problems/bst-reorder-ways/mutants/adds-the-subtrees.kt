// kind: WRONG_ALGORITHM
// 두 서브트리의 순서 수를 더한다. 왼쪽 순서마다 오른쪽 순서를 모두 고를 수 있으니 곱해야 한다.
fun bstReorderWays(nums: IntArray): Int {
    val mod = 1_000_000_007L
    val n = nums.size
    val fact = LongArray(n + 1); fact[0] = 1
    for (i in 1..n) fact[i] = fact[i - 1] * i % mod
    fun power(base: Long, exp: Long): Long { var r = 1L; var b = base % mod; var e = exp; while (e > 0) { if (e and 1L == 1L) r = r * b % mod; b = b * b % mod; e = e shr 1 }; return r }
    fun choose(a: Int, b: Int): Long = fact[a] * power(fact[b], mod - 2) % mod * power(fact[a - b], mod - 2) % mod
    fun ways(xs: List<Int>): Long {
        if (xs.size <= 2) return 1
        val left = xs.filter { it < xs[0] }; val right = xs.filter { it > xs[0] }
        return choose(xs.size - 1, left.size) * ((ways(left) + ways(right)) % mod) % mod
    }
    return ((ways(nums.toList()) - 1 + mod) % mod).toInt()
}
