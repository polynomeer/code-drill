// 검증용 정답 (§6.1 solutions/). 두 칸만 기억하는 반복.
//
// 나머지를 **더할 때마다** 취한다. 마지막에 한 번만 취하면 그 전에 이미 Int 를 넘어
// 값이 망가진다 — n 이 46 만 되어도 그렇다.
fun climbStairs(n: Int): Int {
    val mod = 1_000_000_007L
    var previous = 1L
    var current = 1L

    for (step in 1 until n) {
        val next = (previous + current) % mod
        previous = current
        current = next
        Drill.write(step, current.toInt())
    }
    return current.toInt()
}
