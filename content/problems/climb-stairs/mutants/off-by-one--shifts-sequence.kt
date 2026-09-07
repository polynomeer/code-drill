// kind: OFF_BY_ONE
// 한 칸 덜 올라 n-1 의 답을 돌려준다.
fun climbStairs(n: Int): Int {
    val mod = 1_000_000_007L
    var previous = 1L
    var current = 1L
    for (step in 1 until n - 1) {
        val next = (previous + current) % mod
        previous = current
        current = next
    }
    return current.toInt()
}
