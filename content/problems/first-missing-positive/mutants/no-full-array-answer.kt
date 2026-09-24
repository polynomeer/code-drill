// kind: WRONG_BRANCH
// 자리가 다 맞았을 때 n+1 대신 n 을 돌려준다. 1..n 이 꽉 찬 입력에서 어긋난다.
fun firstMissingPositive(nums: IntArray): Int {
    val n = nums.size
    for (i in 0 until n) {
        while (nums[i] in 1..n && nums[nums[i] - 1] != nums[i]) {
            val target = nums[i] - 1
            val temp = nums[target]
            nums[target] = nums[i]
            nums[i] = temp
        }
    }
    for (i in 0 until n) if (nums[i] != i + 1) return i + 1
    return n
}
