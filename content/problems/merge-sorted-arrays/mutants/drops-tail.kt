// kind: MISSING_EDGE_CASE
// 한쪽이 끝나면 멈춘다. 남은 쪽을 붙이지 않는다.
fun mergeSorted(first: IntArray, second: IntArray): IntArray {
    val out = mutableListOf<Int>()
    var i = 0; var j = 0
    while (i < first.size && j < second.size) {
        out.add(if (first[i] <= second[j]) first[i++] else second[j++])
    }
    return out.toIntArray()
}
