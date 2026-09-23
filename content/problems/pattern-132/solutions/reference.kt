// 검증용 정답 (§6.1 solutions/). 오른쪽에서 왼쪽으로, 밀려난 값 중 가장 큰 것을 기억한다.
fun hasPattern132(nums: IntArray): Int {
    val stack = IntArray(nums.size)
    var top = 0
    var third = Int.MIN_VALUE
    var hasThird = false
    for (i in nums.indices.reversed()) {
        val x = nums[i]
        if (hasThird && x < third) return 1
        while (top > 0 && stack[top - 1] < x) {
            third = stack[--top]
            hasThird = true
            Drill.pop(third)
        }
        stack[top++] = x
        Drill.push(x)
    }
    return 0
}
