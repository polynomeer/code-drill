// kind: WRONG_ALGORITHM
// 밀려난 값 중 처음 것을 세 번째 값으로 삼는다. 더 큰 값이 밀려나도 갱신하지 않아 놓친다.
fun hasPattern132(nums: IntArray): Int {
    val stack = IntArray(nums.size)
    var top = 0
    var third = Int.MIN_VALUE
    var hasThird = false
    for (i in nums.indices.reversed()) {
        val x = nums[i]
        if (hasThird && x < third) return 1
        while (top > 0 && stack[top - 1] < x) {
            val popped = stack[--top]
            if (!hasThird) { third = popped; hasThird = true }
        }
        stack[top++] = x
    }
    return 0
}
