// kind: WRONG_BRANCH
// 같은 값도 역순으로 센다.
fun countInversions(nums: IntArray): Int {
    var count = 0
    for (i in nums.indices) {
        for (j in i + 1 until nums.size) {
            if (nums[i] >= nums[j]) count += 1
        }
    }
    return count
}
