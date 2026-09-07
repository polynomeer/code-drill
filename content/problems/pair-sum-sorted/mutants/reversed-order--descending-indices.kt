// kind: WRONG_BRANCH
// 인덱스를 내림차순으로 돌려준다.
fun pairSum(nums: IntArray, target: Int): IntArray {
    var left = 0
    var right = nums.size - 1
    while (left < right) {
        val total = nums[left] + nums[right]
        if (total == target) return intArrayOf(right, left)
        if (total < target) left += 1 else right -= 1
    }
    error("정답은 항상 존재한다")
}
