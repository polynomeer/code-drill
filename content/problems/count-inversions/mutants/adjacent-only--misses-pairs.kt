// kind: MISSING_EDGE_CASE
// 이웃한 쌍만 센다.
fun countInversions(nums: IntArray): Int {
    var count = 0
    for (i in 0 until nums.size - 1) if (nums[i] > nums[i + 1]) count += 1
    return count
}
