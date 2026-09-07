// kind: PERFORMANCE
// 메모이제이션 없는 재귀라 같은 부분 문제를 지수적으로 다시 푼다.
fun climbStairs(n: Int): Int {
    fun ways(step: Int): Long = when {
        step <= 1 -> 1L
        else -> (ways(step - 1) + ways(step - 2)) % 1_000_000_007L
    }
    return ways(n).toInt()
}
