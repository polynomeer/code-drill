// kind: WRONG_BRANCH
// 음수를 만나면 무조건 끊는다. 음수를 건너뛰면 더 큰 합이 되는 경우를 놓친다.
fun maxSubarray(nums: IntArray): Int {
    var current = nums[0]
    var best = nums[0]
    for (i in 1 until nums.size) {
        current = if (nums[i] < 0) nums[i] else current + nums[i]
        if (current > best) best = current
    }
    return best
}
