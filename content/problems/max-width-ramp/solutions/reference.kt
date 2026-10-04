// 검증용 정답 (§6.1 solutions/). 왼쪽 끝 후보의 줄어드는 스택, 오른쪽 끝은 뒤에서부터.
fun maxWidthRamp(nums: IntArray): Int {
    val stack = IntArray(nums.size)
    var top = 0
    for (i in nums.indices) {
        if (top == 0 || nums[stack[top - 1]] > nums[i]) {
            stack[top] = i
            top += 1
            Drill.push(i)
        }
    }
    var best = 0
    var j = nums.size - 1
    while (j >= 0 && top > 0) {
        while (top > 0 && nums[stack[top - 1]] <= nums[j]) {
            top -= 1
            Drill.pop(stack[top])
            Drill.compare(stack[top], j)
            best = maxOf(best, j - stack[top])
        }
        j -= 1
    }
    return best
}
