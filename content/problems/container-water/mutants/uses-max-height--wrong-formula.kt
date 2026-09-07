// kind: WRONG_BRANCH
// 두 높이 중 큰 값을 쓴다. 물은 낮은 벽까지만 찬다.
fun maxWater(heights: IntArray): Int {
    var left = 0
    var right = heights.size - 1
    var best = 0
    while (left < right) {
        val area = (right - left) * maxOf(heights[left], heights[right])
        if (area > best) best = area
        if (heights[left] < heights[right]) left += 1 else right -= 1
    }
    return best
}
