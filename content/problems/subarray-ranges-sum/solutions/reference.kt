// 검증용 정답 (§6.1 solutions/). 최댓값의 합 − 최솟값의 합. 원소마다 그 원소가 맡는 구간 수를 단조 스택으로 구한다.
fun subarrayRangesSum(nums: IntArray): Int {
    val mod = 1_000_000_007L
    val n = nums.size
    fun total(isMax: Boolean): Long {
        val left = IntArray(n); val right = IntArray(n)
        val stack = IntArray(n); var size = 0
        for (i in 0 until n) {
            // 왼쪽은 엄격하게, 오른쪽은 등호를 두어 같은 값을 한 원소만 맡게 한다
            while (size > 0 && (if (isMax) nums[stack[size - 1]] < nums[i] else nums[stack[size - 1]] > nums[i])) size -= 1
            left[i] = i - (if (size > 0) stack[size - 1] else -1)
            stack[size++] = i
            Drill.push(i)
        }
        size = 0
        for (i in n - 1 downTo 0) {
            while (size > 0 && (if (isMax) nums[stack[size - 1]] <= nums[i] else nums[stack[size - 1]] >= nums[i])) size -= 1
            right[i] = (if (size > 0) stack[size - 1] else n) - i
            stack[size++] = i
        }
        var sum = 0L
        for (i in 0 until n) {
            val value = ((nums[i] % mod) + mod) % mod
            sum = (sum + value * (left[i].toLong() * right[i] % mod)) % mod
        }
        return sum
    }
    return (((total(true) - total(false)) % mod + mod) % mod).toInt()
}
