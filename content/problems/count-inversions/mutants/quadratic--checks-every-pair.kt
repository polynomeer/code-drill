// kind: PERFORMANCE
// 모든 쌍을 본다. 작은 입력은 통과한다.
fun countInversions(nums: IntArray): Int {
    var count = 0
    for (i in nums.indices) {
        for (j in i + 1 until nums.size) {
            if (nums[i] > nums[j]) count += 1
        }
    }
    return count
}
