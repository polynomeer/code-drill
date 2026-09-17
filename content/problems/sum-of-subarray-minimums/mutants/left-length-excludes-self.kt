// kind: OFF_BY_ONE
// 왼쪽 길이를 자기 자신을 빼고 센다. 원소 하나짜리 부분 배열이 사라진다.
fun sumOfSubarrayMinimums(nums: IntArray): Int {
    val n = nums.size; val mod = 1_000_000_007L
    val left = IntArray(n); val right = IntArray(n); val stack = IntArray(n); var top = 0
    for (i in 0 until n) { while (top > 0 && nums[stack[top - 1]] > nums[i]) top -= 1; left[i] = i - (if (top > 0) stack[top - 1] else -1) - 1; stack[top++] = i }
    top = 0
    for (i in n - 1 downTo 0) { while (top > 0 && nums[stack[top - 1]] >= nums[i]) top -= 1; right[i] = (if (top > 0) stack[top - 1] else n) - i; stack[top++] = i }
    var total = 0L
    for (i in 0 until n) total = (total + nums[i].toLong() * (left[i] + 1) * right[i] - nums[i].toLong() * right[i]) % mod
    return total.toInt()
}
