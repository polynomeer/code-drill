// kind: MISSING_EDGE_CASE
// 마지막에만 나머지를 취해 그 전에 Int 가 넘친다.
fun climbStairs(n: Int): Int {
    var previous = 1
    var current = 1
    for (step in 1 until n) {
        val next = previous + current
        previous = current
        current = next
    }
    return current % 1_000_000_007
}
