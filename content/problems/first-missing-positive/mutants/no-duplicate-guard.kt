// kind: MISSING_EDGE_CASE
// 보낼 자리에 같은 값이 이미 있는지 보지 않는다. 중복이 있으면 두 자리를 영원히 맞바꾼다.
fun firstMissingPositive(nums: IntArray): Int {
    val n = nums.size
    for (i in 0 until n) {
        while (nums[i] in 1..n && nums[i] != i + 1) {
            val target = nums[i] - 1
            val temp = nums[target]
            nums[target] = nums[i]
            nums[i] = temp
        }
    }
    for (i in 0 until n) if (nums[i] != i + 1) return i + 1
    return n + 1
}
