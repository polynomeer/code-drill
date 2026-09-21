// kind: WRONG_ALGORITHM
// 행마다 1차원 빗물 문제를 풀어 더한다. 물은 위아래로도 새어 나간다.
fun trappingRainWater2d(heights: Array<IntArray>): Int {
    val rows = heights.size
    if (rows < 3) return 0
    var water = 0
    for (r in 1 until rows - 1) {
        val row = heights[r]
        var lo = 0; var hi = row.size - 1; var leftMax = 0; var rightMax = 0
        while (lo < hi) {
            if (row[lo] < row[hi]) { leftMax = maxOf(leftMax, row[lo]); water += leftMax - row[lo]; lo += 1 }
            else { rightMax = maxOf(rightMax, row[hi]); water += rightMax - row[hi]; hi -= 1 }
        }
    }
    return water
}
