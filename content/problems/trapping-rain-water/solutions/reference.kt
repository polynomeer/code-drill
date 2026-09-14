// 검증용 정답 (§6.1 solutions/). 양끝 포인터 — 낮은 쪽을 옮긴다.
fun trappedWater(heights: IntArray): Int {
    var left = 0
    var right = heights.size - 1
    var leftMax = 0
    var rightMax = 0
    var total = 0
    while (left < right) {
        if (heights[left] < heights[right]) {
            leftMax = maxOf(leftMax, heights[left])
            total += leftMax - heights[left]
            Drill.write(left, leftMax - heights[left])
            left += 1
            Drill.pointer("left", left)
        } else {
            rightMax = maxOf(rightMax, heights[right])
            total += rightMax - heights[right]
            Drill.write(right, rightMax - heights[right])
            right -= 1
            Drill.pointer("right", right)
        }
    }
    return total
}
