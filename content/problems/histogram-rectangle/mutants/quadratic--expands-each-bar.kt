// kind: PERFORMANCE
// 막대마다 좌우로 뻗어 O(n^2) 다. 오름차순 입력이 최악이다.
fun largestRectangle(heights: IntArray): Int {
    var best = 0
    for (index in heights.indices) {
        var height = heights[index]
        var left = index
        var right = index
        while (left > 0 && heights[left - 1] >= height) left -= 1
        while (right < heights.size - 1 && heights[right + 1] >= height) right += 1
        val area = height * (right - left + 1)
        if (area > best) best = area
    }
    return best
}
