// kind: OFF_BY_ONE
// 마지막 원소 뒤에서도 자른다. 그러면 오른쪽이 비고, 전체 합이 0 이면 그것도 세어진다.
fun balancedSplits(nums: IntArray): Int {
    val total = nums.sumOf { it.toLong() }
    var left = 0L; var count = 0
    for (i in nums.indices) { left += nums[i]; if (2 * left == total) count += 1 }
    return count
}
