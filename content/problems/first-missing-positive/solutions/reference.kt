// 검증용 정답 (§6.1 solutions/). 값 v 를 v-1 번 자리로 보내고, 자리와 값이 어긋난 첫 곳을 찾는다.
fun firstMissingPositive(nums: IntArray): Int {
    val n = nums.size
    for (i in 0 until n) {
        while (nums[i] in 1..n && nums[nums[i] - 1] != nums[i]) {
            val target = nums[i] - 1
            val temp = nums[target]
            nums[target] = nums[i]
            nums[i] = temp
            Drill.swap(i, target)
        }
    }
    for (i in 0 until n) {
        Drill.visit(i, nums[i])
        if (nums[i] != i + 1) return i + 1
    }
    return n + 1
}
