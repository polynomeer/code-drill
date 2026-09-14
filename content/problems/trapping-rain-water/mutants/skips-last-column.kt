// kind: OFF_BY_ONE
// 포인터가 만나는 마지막 칸을 계산하지 않는다 — 그 칸은 물이 0 이라 답은 같다. 대신 첫 칸을 건너뛴다.
fun trappedWater(heights: IntArray): Int {
    if (heights.size < 3) return 0
    var left = 1; var right = heights.size - 1
    var leftMax = heights[1]; var rightMax = 0
    var total = 0
    while (left < right) {
        if (heights[left] < heights[right]) {
            leftMax = maxOf(leftMax, heights[left]); total += leftMax - heights[left]; left += 1
        } else {
            rightMax = maxOf(rightMax, heights[right]); total += rightMax - heights[right]; right -= 1
        }
    }
    return total
}
