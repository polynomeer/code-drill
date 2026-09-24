// kind: OFF_BY_ONE
// 길이가 둘인 배열을 거른 것으로 친다. 꼭대기 자리 검사가 그 자리를 못 막는다.
fun isMountain(nums: IntArray): Int {
    val n = nums.size
    if (n < 2) return 0
    var i = 0
    while (i + 1 < n && nums[i] < nums[i + 1]) i += 1
    if (i == 0) return 0
    while (i + 1 < n && nums[i] > nums[i + 1]) i += 1
    return if (i == n - 1) 1 else 0
}
