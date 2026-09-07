// kind: OFF_BY_ONE
// 0 부터가 아니라 1 부터 세어 돌려준다. 같은 문제의 다른 판본을 기억해 두면 나는 실수다.
fun pairSum(nums: IntArray, target: Int): IntArray {
    var left = 0
    var right = nums.size - 1
    while (left < right) {
        val total = nums[left] + nums[right]
        if (total == target) return intArrayOf(left + 1, right + 1)
        if (total < target) left += 1 else right -= 1
    }
    error("정답은 항상 존재한다")
}
