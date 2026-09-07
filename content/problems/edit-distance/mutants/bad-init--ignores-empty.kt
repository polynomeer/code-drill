// kind: MISSING_EDGE_CASE
// 빈 수열로 시작하는 줄을 0 으로 두어 삽입·삭제 비용을 세지 않는다.
fun editDistance(source: IntArray, target: IntArray): Int {
    var previous = IntArray(target.size + 1)
    val current = IntArray(target.size + 1)
    for (i in 1..source.size) {
        current[0] = 0
        for (j in 1..target.size) {
            val cost = if (source[i - 1] == target[j - 1]) 0 else 1
            current[j] = minOf(previous[j] + 1, current[j - 1] + 1, previous[j - 1] + cost)
        }
        for (j in previous.indices) previous[j] = current[j]
    }
    return previous[target.size]
}
