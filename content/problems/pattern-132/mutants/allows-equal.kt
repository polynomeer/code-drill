// kind: WRONG_BRANCH
// 세 값이 같아도 패턴으로 친다. 사이에 있는 값은 양끝과 달라야 한다.
fun hasPattern132(nums: IntArray): Int {
    val stack = IntArray(nums.size)
    var top = 0
    var third = Int.MIN_VALUE
    var hasThird = false
    for (i in nums.indices.reversed()) {
        val x = nums[i]
        if (hasThird && x <= third) return 1
        while (top > 0 && stack[top - 1] < x) { third = stack[--top]; hasThird = true }
        stack[top++] = x
    }
    return 0
}
