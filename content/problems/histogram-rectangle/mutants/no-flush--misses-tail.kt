// kind: MISSING_EDGE_CASE
// 마지막에 스택을 비우지 않아 오른쪽 끝까지 뻗는 직사각형을 놓친다.
fun largestRectangle(heights: IntArray): Int {
    val stack = ArrayDeque<Int>()
    var best = 0
    for (index in heights.indices) {
        while (stack.isNotEmpty() && heights[stack.last()] >= heights[index]) {
            val top = stack.removeLast()
            val left = if (stack.isEmpty()) 0 else stack.last() + 1
            val area = heights[top] * (index - left)
            if (area > best) best = area
        }
        stack.addLast(index)
    }
    return best
}
