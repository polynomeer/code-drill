// kind: PERFORMANCE
// 칸마다 왼쪽과 오른쪽 전체를 훑어 최대를 찾는다. O(n²).
fun trappedWater(heights: IntArray): Int {
    var total = 0
    for (i in heights.indices) {
        var leftMax = 0; var rightMax = 0
        for (j in 0..i) { Drill.compare(i, j); leftMax = maxOf(leftMax, heights[j]) }
        for (j in i until heights.size) rightMax = maxOf(rightMax, heights[j])
        total += minOf(leftMax, rightMax) - heights[i]
    }
    return total
}
