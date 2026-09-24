// kind: WRONG_ALGORITHM
// 오르막만 확인하고 내리막이 끝까지 가는지 보지 않는다.
fun isMountain(nums: IntArray): Int {
    val n = nums.size
    var i = 0
    while (i + 1 < n && nums[i] < nums[i + 1]) i += 1
    return if (i > 0 && i < n - 1) 1 else 0
}
