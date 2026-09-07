// kind: MISSING_EDGE_CASE
// 막대 하나씩만 본다. 낮지만 넓은 직사각형을 놓친다.
fun largestRectangle(heights: IntArray): Int {
    var best = 0
    for (height in heights) if (height > best) best = height
    return best
}
