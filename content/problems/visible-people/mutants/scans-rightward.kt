// kind: PERFORMANCE
// 사람마다 오른쪽을 훑으며 지금까지 본 가장 큰 키를 넘는 사람을 센다. 키가 줄어들면 끝까지 훑는다.
fun visiblePeople(heights: IntArray): IntArray {
    val n = heights.size
    return IntArray(n) { i ->
        var seen = 0
        var tallest = 0
        var j = i + 1
        while (j < n) {
            Drill.compare(i, j)
            if (heights[j] > tallest) { seen += 1; tallest = heights[j] }
            if (heights[j] > heights[i]) break
            j += 1
        }
        seen
    }
}
