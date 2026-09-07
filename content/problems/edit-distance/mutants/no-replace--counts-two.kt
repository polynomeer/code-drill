// kind: MISSING_EDGE_CASE
// 교체를 삭제+삽입으로 센다. 한 글자만 다른 경우가 2 가 된다.
fun editDistance(source: IntArray, target: IntArray): Int {
    var previous = IntArray(target.size + 1) { it }
    val current = IntArray(target.size + 1)
    for (i in 1..source.size) {
        current[0] = i
        for (j in 1..target.size) {
            current[j] = if (source[i - 1] == target[j - 1]) {
                previous[j - 1]
            } else {
                minOf(previous[j] + 1, current[j - 1] + 1)
            }
        }
        for (j in previous.indices) previous[j] = current[j]
    }
    return previous[target.size]
}
