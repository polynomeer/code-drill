// kind: WRONG_ALGORITHM
// 같아지는 자리를 하나 찾으면 1 을 낸다. 0 이 이어지면 여러 자리에서 같아진다.
fun balancedSplits(nums: IntArray): Int {
    val total = nums.sumOf { it.toLong() }
    var left = 0L
    for (i in 0 until nums.size - 1) { left += nums[i]; if (2 * left == total) return 1 }
    return 0
}
