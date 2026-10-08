// kind: WRONG_ALGORITHM
// 트리 전체에서 같은 값의 쌍을 모두 센다. 사이에 더 큰 값이 있으면 좋은 경로가 아니다.
fun goodPaths(vals: IntArray, edges: IntArray): Int {
    val counts = HashMap<Int, Long>()
    for (v in vals) counts[v] = (counts[v] ?: 0L) + 1
    var total = 0L
    for (c in counts.values) total += c * (c + 1) / 2
    return total.toInt()
}
