// kind: WRONG_BRANCH
// 같은 값이 이어져도 오르막·내리막으로 친다. 산은 엄격히 늘고 엄격히 줄어야 한다.
fun isMountain(nums: IntArray): Int {
    val n = nums.size
    var i = 0
    while (i + 1 < n && nums[i] <= nums[i + 1]) i += 1
    if (i == 0 || i == n - 1) return 0
    while (i + 1 < n && nums[i] >= nums[i + 1]) i += 1
    return if (i == n - 1) 1 else 0
}
