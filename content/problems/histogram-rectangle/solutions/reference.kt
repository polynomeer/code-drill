// 검증용 정답 (§6.1 solutions/). 단조 증가 스택.
//
// 스택에는 "아직 오른쪽 경계를 못 만난 막대"만 남는다. 더 낮은 막대가 오면 그것이
// 경계이므로, 그 순간 넓이를 확정할 수 있다.
//
// 마지막에 높이 0 을 한 번 더 흘려보내 스택을 비운다. 그러지 않으면 오른쪽 끝까지
// 뻗는 직사각형이 계산되지 않는다.
fun largestRectangle(heights: IntArray): Int {
    val stack = ArrayDeque<Int>()
    var best = 0

    for (index in 0..heights.size) {
        val current = if (index == heights.size) 0 else heights[index]

        while (stack.isNotEmpty() && heights[stack.last()] >= current) {
            val top = stack.removeLast()
            Drill.pop(top)
            val height = heights[top]
            val left = if (stack.isEmpty()) 0 else stack.last() + 1
            val area = height * (index - left)
            if (area > best) {
                best = area
                Drill.write(0, best)
            }
        }
        stack.addLast(index)
        Drill.push(index)
    }
    return best
}
