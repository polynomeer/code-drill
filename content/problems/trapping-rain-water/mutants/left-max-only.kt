// kind: WRONG_ALGORITHM
// 왼쪽 최대 높이만 본다. 오른쪽 벽이 더 낮으면 넘치는 물까지 센다.
fun trappedWater(heights: IntArray): Int {
    var leftMax = 0
    var total = 0
    for (h in heights) {
        leftMax = maxOf(leftMax, h)
        total += leftMax - h
    }
    return total
}
