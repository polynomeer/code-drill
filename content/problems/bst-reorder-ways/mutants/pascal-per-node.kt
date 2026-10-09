// kind: PERFORMANCE
// 노드마다 파스칼 삼각형을 처음부터 쌓아 이항 계수를 구한다. 맞지만, 깊은 트리에서는 크기의 세제곱이다.
fun bstReorderWays(nums: IntArray): Int {
    val mod = 1_000_000_007L
    fun choose(a: Int, b: Int): Long {
        var row = LongArray(1) { 1L }
        for (r in 1..a) {
            val next = LongArray(r + 1)
            next[0] = 1; next[r] = 1
            for (j in 1 until r) next[j] = (row[j - 1] + row[j]) % mod
            row = next
        }
        return row[b]
    }
    var total = 1L
    val stack = ArrayDeque<IntArray>()
    stack.addLast(nums)
    while (stack.isNotEmpty()) {
        val xs = stack.removeLast()
        if (xs.size <= 2) continue
        val left = xs.filter { it < xs[0] }.toIntArray(); val right = xs.filter { it > xs[0] }.toIntArray()
        total = total * choose(xs.size - 1, left.size) % mod
        stack.addLast(left); stack.addLast(right)
    }
    return ((total - 1 + mod) % mod).toInt()
}
