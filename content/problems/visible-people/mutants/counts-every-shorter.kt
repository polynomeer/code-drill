// kind: WRONG_ALGORITHM
// 오른쪽의 자기보다 작은 사람을 모두 센다. 사이에 더 큰 사람이 있으면 가려진다.
fun visiblePeople(heights: IntArray): IntArray {
    val n = heights.size
    return IntArray(n) { i ->
        var seen = 0
        for (j in i + 1 until n) {
            if (heights[j] < heights[i]) seen += 1 else { seen += 1; break }
        }
        seen
    }
}
