// 검증용 정답 (§6.1 solutions/). 오른쪽에서부터 키가 줄어드는 스택.
fun visiblePeople(heights: IntArray): IntArray {
    val n = heights.size
    val out = IntArray(n)
    val stack = IntArray(n)
    var top = 0
    for (i in n - 1 downTo 0) {
        var seen = 0
        while (top > 0 && stack[top - 1] < heights[i]) {
            top -= 1
            Drill.pop(stack[top])
            seen += 1
        }
        if (top > 0) seen += 1
        out[i] = seen
        stack[top] = heights[i]
        top += 1
        Drill.push(heights[i])
    }
    return out
}
