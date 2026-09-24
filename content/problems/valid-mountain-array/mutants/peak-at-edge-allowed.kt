// kind: MISSING_EDGE_CASE
// 꼭대기가 첫 칸이나 마지막 칸이어도 산으로 친다. 양쪽이 모두 있어야 한다.
fun isMountain(nums: IntArray): Int {
    val n = nums.size
    var i = 0
    while (i + 1 < n && nums[i] < nums[i + 1]) i += 1
    while (i + 1 < n && nums[i] > nums[i + 1]) i += 1
    return if (i == n - 1) 1 else 0
}
