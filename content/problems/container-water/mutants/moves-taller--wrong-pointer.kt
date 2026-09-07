// kind: WRONG_BRANCH
// 낮은 쪽이 아니라 높은 쪽을 옮겨, 더 나은 후보를 지나쳐 버린다.
fun maxWater(heights: IntArray): Int {
    var left = 0
    var right = heights.size - 1
    var best = 0
    while (left < right) {
        val area = (right - left) * minOf(heights[left], heights[right])
        if (area > best) best = area
        if (heights[left] > heights[right]) left += 1 else right -= 1
    }
    return best
}
