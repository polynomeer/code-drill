// kind: MISSING_EDGE_CASE
// 같은 원소를 두 번 쓴다. target 이 짝수이고 절반 값이 있는 케이스에서 드러난다.
fun twoSum(nums: IntArray, target: Int): IntArray {
    for (i in nums.indices) {
        for (j in nums.indices) {
            if (nums[i] + nums[j] == target) return intArrayOf(minOf(i, j), maxOf(i, j))
        }
    }
    error("정답은 항상 존재한다")
}
