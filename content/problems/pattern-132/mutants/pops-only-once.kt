// kind: OFF_BY_ONE
// 한 값이 여럿을 밀어내는 자리에서 하나만 꺼낸다. 스택에 더 작은 값이 남아 뒤가 어긋난다.
fun hasPattern132(nums: IntArray): Int {
    val stack = IntArray(nums.size)
    var top = 0
    var third = Int.MIN_VALUE
    var hasThird = false
    for (i in nums.indices.reversed()) {
        val x = nums[i]
        if (hasThird && x < third) return 1
        if (top > 0 && stack[top - 1] < x) { third = stack[--top]; hasThird = true }
        stack[top++] = x
    }
    return 0
}
