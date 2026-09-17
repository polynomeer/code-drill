// kind: WRONG_BRANCH
// 양쪽 모두 엄격히 작은 값에서 멈춘다. 같은 값이 있는 부분 배열을 두 번 센다.
fun sumOfSubarrayMinimums(nums: IntArray): Int {
    val n = nums.size; val mod = 1_000_000_007L
    val left = IntArray(n); val right = IntArray(n); val stack = IntArray(n); var top = 0
    for (i in 0 until n) { while (top > 0 && nums[stack[top - 1]] > nums[i]) top -= 1; left[i] = i - (if (top > 0) stack[top - 1] else -1); stack[top++] = i }
    top = 0
    for (i in n - 1 downTo 0) { while (top > 0 && nums[stack[top - 1]] > nums[i]) top -= 1; right[i] = (if (top > 0) stack[top - 1] else n) - i; stack[top++] = i }
    var total = 0L
    for (i in 0 until n) total = (total + nums[i].toLong() * left[i] * right[i]) % mod
    return total.toInt()
}
