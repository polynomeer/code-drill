// 검증용 정답 (§6.1 solutions/). 루트를 떼고 작은 쪽·큰 쪽으로 나눈 뒤, 둘을 섞는 자리의 수 C(m, |왼쪽|) 를 곱한다.
fun bstReorderWays(nums: IntArray): Int {
    val mod = 1_000_000_007L
    val n = nums.size
    val fact = LongArray(n + 1)
    fact[0] = 1
    for (i in 1..n) fact[i] = fact[i - 1] * i % mod
    fun power(base: Long, exp: Long): Long {
        var result = 1L; var b = base % mod; var e = exp
        while (e > 0) { if (e and 1L == 1L) result = result * b % mod; b = b * b % mod; e = e shr 1 }
        return result
    }
    val inverse = LongArray(n + 1) { power(fact[it], mod - 2) }
    fun choose(a: Int, b: Int): Long = fact[a] * inverse[b] % mod * inverse[a - b] % mod
    // 정렬된 입력이면 깊이가 n 이다 — 재귀 대신 스택으로 돈다.
    var total = 1L
    val stack = ArrayDeque<IntArray>()
    stack.addLast(nums)
    while (stack.isNotEmpty()) {
        val xs = stack.removeLast()
        if (xs.size <= 2) continue
        val root = xs[0]
        val left = xs.filter { it < root }.toIntArray()
        val right = xs.filter { it > root }.toIntArray()
        val here = choose(xs.size - 1, left.size)
        Drill.ret("ways($root)", here.toInt())
        total = total * here % mod
        stack.addLast(left); stack.addLast(right)
    }
    return ((total - 1 + mod) % mod).toInt()
}
