// kind: WRONG_BRANCH
// 오른쪽의 첫 사람만 보고 멈춘다. 그보다 큰 사람이 뒤에 오면 그 사람도 보인다.
fun visiblePeople(heights: IntArray): IntArray {
    val n = heights.size
    return IntArray(n) { i -> if (i + 1 < n) 1 else 0 }
}
