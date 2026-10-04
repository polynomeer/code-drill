// kind: OFF_BY_ONE
// 같은 값을 오르막으로 보지 않는다. 문제는 작거나 같으면 오르막이다.
fun maxWidthRamp(nums: IntArray): Int {
    val stack = IntArray(nums.size)
    var top = 0
    for (i in nums.indices) if (top == 0 || nums[stack[top - 1]] > nums[i]) { stack[top] = i; top += 1 }
    var best = 0
    for (j in nums.indices.reversed()) {
        while (top > 0 && nums[stack[top - 1]] < nums[j]) { top -= 1; best = maxOf(best, j - stack[top]) }
    }
    return best
}
