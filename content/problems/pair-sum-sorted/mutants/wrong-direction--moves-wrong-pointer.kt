// kind: WRONG_BRANCH
// 합이 작을 때 오른쪽을 당긴다. 답이 양 끝에 있는 경우에만 우연히 맞는다.
fun pairSum(nums: IntArray, target: Int): IntArray {
    var left = 0
    var right = nums.size - 1
    while (left < right) {
        val total = nums[left] + nums[right]
        if (total == target) return intArrayOf(left, right)
        if (total < target) right -= 1 else left += 1
    }
    error("정답은 항상 존재한다")
}
