// kind: WRONG_BRANCH
// 합이 맞는 순간 그 가지 아래를 더 보지 않는다. 0 을 더해 같은 합이 되는 집합을 놓친다.
fun subsetCount(nums: IntArray, target: Int): Int {
    fun go(i: Int, remaining: Int): Int {
        if (remaining == 0) return 1
        if (i == nums.size) return 0
        return go(i + 1, remaining - nums[i]) + go(i + 1, remaining)
    }
    return go(0, target)
}
