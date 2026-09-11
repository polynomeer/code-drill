// kind: WRONG_BRANCH
// 남은 합이 음수면 가지를 친다. 음수 원소가 있으면 그 뒤에서 다시 커질 수 있다.
fun subsetCount(nums: IntArray, target: Int): Int {
    fun go(i: Int, remaining: Int): Int {
        if (remaining < 0) return 0
        if (i == nums.size) return if (remaining == 0) 1 else 0
        return go(i + 1, remaining - nums[i]) + go(i + 1, remaining)
    }
    return go(0, target)
}
