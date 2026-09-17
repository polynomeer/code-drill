// kind: PERFORMANCE
// 모든 부분 배열의 최솟값을 직접 본다. O(n²).
fun sumOfSubarrayMinimums(nums: IntArray): Int {
    val mod = 1_000_000_007L
    var total = 0L
    for (i in nums.indices) {
        var m = Int.MAX_VALUE
        for (j in i until nums.size) { Drill.compare(i, j); if (nums[j] < m) m = nums[j]; total = (total + m) % mod }
    }
    return total.toInt()
}
