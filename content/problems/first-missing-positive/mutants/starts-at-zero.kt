// kind: OFF_BY_ONE
// 0 부터 세어 자리와 값을 맞춘다. 양의 정수만 세는 문제다.
fun firstMissingPositive(nums: IntArray): Int {
    val n = nums.size
    for (i in 0 until n) {
        while (nums[i] in 0 until n && nums[nums[i]] != nums[i]) {
            val target = nums[i]
            val temp = nums[target]
            nums[target] = nums[i]
            nums[i] = temp
        }
    }
    for (i in 0 until n) if (nums[i] != i) return i
    return n
}
