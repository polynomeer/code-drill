// 검증용 정답 (§6.1 solutions/). 투 포인터.
//
// 낮은 쪽 벽을 옮긴다. 낮은 벽은 자기가 낀 어떤 그릇에서도 높이를 결정하므로,
// 폭이 줄어드는 쪽으로는 더 나은 답이 나올 수 없다.
fun maxWater(heights: IntArray): Int {
    var left = 0
    var right = heights.size - 1
    var best = 0

    while (left < right) {
        Drill.pointer("left", left)
        Drill.pointer("right", right)

        val area = (right - left) * minOf(heights[left], heights[right])
        if (area > best) {
            best = area
            Drill.write(0, best)
        }
        if (heights[left] < heights[right]) left += 1 else right -= 1
    }
    return best
}
