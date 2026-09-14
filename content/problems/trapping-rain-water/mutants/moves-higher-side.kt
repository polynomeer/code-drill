// kind: WRONG_BRANCH
// 높은 쪽 포인터를 옮긴다. 물의 높이가 아직 정해지지 않은 칸을 계산한다.
fun trappedWater(heights: IntArray): Int {
    var left = 0; var right = heights.size - 1
    var leftMax = 0; var rightMax = 0
    var total = 0
    while (left < right) {
        if (heights[left] >= heights[right]) {
            leftMax = maxOf(leftMax, heights[left]); total += leftMax - heights[left]; left += 1
        } else {
            rightMax = maxOf(rightMax, heights[right]); total += rightMax - heights[right]; right -= 1
        }
    }
    return total
}
