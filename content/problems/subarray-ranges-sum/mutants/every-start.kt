// kind: PERFORMANCE
// 시작마다 끝을 늘려 가며 최댓값과 최솟값을 갱신한다. 맞지만, 10 만이면 50 억 구간이다.
fun subarrayRangesSum(nums: IntArray): Int {
    val mod = 1_000_000_007L
    var total = 0L
    for (start in nums.indices) {
        var high = nums[start]; var low = nums[start]
        for (end in start until nums.size) {
            if (nums[end] > high) high = nums[end]
            if (nums[end] < low) low = nums[end]
            total = (total + (high.toLong() - low)) % mod
        }
    }
    return total.toInt()
}
