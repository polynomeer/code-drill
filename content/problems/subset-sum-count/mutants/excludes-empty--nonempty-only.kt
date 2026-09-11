// kind: MISSING_EDGE_CASE
// 빈 부분집합을 세지 않는다. target 이 0 일 때 하나 적다.
fun subsetCount(nums: IntArray, target: Int): Int {
    fun go(i: Int, remaining: Int, taken: Int): Int {
        if (i == nums.size) return if (remaining == 0 && taken > 0) 1 else 0
        return go(i + 1, remaining - nums[i], taken + 1) + go(i + 1, remaining, taken)
    }
    return go(0, target, 0)
}
