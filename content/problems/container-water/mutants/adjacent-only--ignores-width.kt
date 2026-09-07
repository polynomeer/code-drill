// kind: MISSING_EDGE_CASE
// 붙어 있는 두 벽만 본다. 폭이 넓은 그릇을 놓친다.
fun maxWater(heights: IntArray): Int {
    var best = 0
    for (i in 0 until heights.size - 1) {
        val area = minOf(heights[i], heights[i + 1])
        if (area > best) best = area
    }
    return best
}
