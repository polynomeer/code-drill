// kind: PERFORMANCE
// 구간을 재귀로 풀되 기억하지 않는다. 지수적이다.
fun burstBalloons(nums: IntArray): Int {
    val vals = IntArray(nums.size + 2) { 1 }
    for (i in nums.indices) vals[i + 1] = nums[i]
    fun solve(left: Int, right: Int): Int {
        if (right - left < 2) return 0
        var top = 0
        for (k in left + 1 until right) { Drill.compare(k, 0); top = maxOf(top, solve(left, k) + solve(k, right) + vals[left] * vals[k] * vals[right]) }
        return top
    }
    return solve(0, vals.size - 1)
}
