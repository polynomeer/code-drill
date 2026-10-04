// kind: WRONG_BRANCH
// 오른쪽 끝 하나에 왼쪽 끝을 하나만 꺼낸다. 같은 오른쪽 끝이 더 앞의 후보와도 짝지어진다.
fun maxWidthRamp(nums: IntArray): Int {
    val stack = IntArray(nums.size)
    var top = 0
    for (i in nums.indices) if (top == 0 || nums[stack[top - 1]] > nums[i]) { stack[top] = i; top += 1 }
    var best = 0
    for (j in nums.indices.reversed()) {
        if (top > 0 && nums[stack[top - 1]] <= nums[j]) { top -= 1; best = maxOf(best, j - stack[top]) }
    }
    return best
}
