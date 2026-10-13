// kind: MISSING_EDGE_CASE
// 음수 값과 뺄셈 뒤의 나머지를 양수로 되돌리지 않는다. 음수 나머지가 그대로 나온다.
fun subarrayRangesSum(nums: IntArray): Int {
    val mod = 1_000_000_007L
    val n = nums.size
    fun total(isMax: Boolean): Long {
        val left = IntArray(n); val right = IntArray(n); val stack = IntArray(n); var size = 0
        for (i in 0 until n) {
            while (size > 0 && (if (isMax) nums[stack[size - 1]] < nums[i] else nums[stack[size - 1]] > nums[i])) size -= 1
            left[i] = i - (if (size > 0) stack[size - 1] else -1); stack[size++] = i
        }
        size = 0
        for (i in n - 1 downTo 0) {
            while (size > 0 && (if (isMax) nums[stack[size - 1]] <= nums[i] else nums[stack[size - 1]] >= nums[i])) size -= 1
            right[i] = (if (size > 0) stack[size - 1] else n) - i; stack[size++] = i
        }
        var sum = 0L
        for (i in 0 until n) sum = (sum + nums[i] % mod * (left[i].toLong() * right[i] % mod)) % mod
        return sum
    }
    return ((total(true) - total(false)) % mod).toInt()
}
