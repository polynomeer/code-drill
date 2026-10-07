// kind: MISSING_EDGE_CASE
// 자기보다 큰 첫 사람을 세지 않는다. 그 사람은 가려지지 않고 보인다.
fun visiblePeople(heights: IntArray): IntArray {
    val n = heights.size
    val out = IntArray(n)
    val stack = ArrayDeque<Int>()
    for (i in n - 1 downTo 0) {
        var seen = 0
        while (stack.isNotEmpty() && stack.last() < heights[i]) { stack.removeLast(); seen += 1 }
        out[i] = seen
        stack.addLast(heights[i])
    }
    return out
}
