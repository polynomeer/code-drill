// kind: WRONG_BRANCH
// 왼쪽 합을 total / 2 와 비교한다. 전체 합이 홀수면 내림한 절반과 같아도 두 합은 다르다.
fun balancedSplits(nums: IntArray): Int {
    val total = nums.sumOf { it.toLong() }
    var left = 0L; var count = 0
    for (i in 0 until nums.size - 1) { left += nums[i]; if (left == total / 2) count += 1 }
    return count
}
