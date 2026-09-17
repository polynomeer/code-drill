// 검증용 정답 (§6.1 solutions/). 단조 스택으로 양쪽 경계, 기여는 Long.
fun sumOfSubarrayMinimums(nums: IntArray): Int {
    val n = nums.size
    val mod = 1_000_000_007L
    val left = IntArray(n)
    val right = IntArray(n)
    val stack = IntArray(n)
    var top = 0
    for (i in 0 until n) {
        while (top > 0 && nums[stack[top - 1]] > nums[i]) { Drill.compare(stack[top - 1], i); top -= 1 }
        left[i] = i - (if (top > 0) stack[top - 1] else -1)
        stack[top++] = i
    }
    top = 0
    for (i in n - 1 downTo 0) {
        while (top > 0 && nums[stack[top - 1]] >= nums[i]) { Drill.compare(stack[top - 1], i); top -= 1 }
        right[i] = (if (top > 0) stack[top - 1] else n) - i
        stack[top++] = i
    }
    var total = 0L
    for (i in 0 until n) {
        val contribution = nums[i].toLong() * left[i] * right[i] % mod
        Drill.write(i, contribution.toInt())
        total = (total + contribution) % mod
    }
    return total.toInt()
}
