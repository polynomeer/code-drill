// kind: MISSING_EDGE_CASE
// first 의 나머지는 붙이는데 second 의 나머지는 잊는다.
fun mergeSorted(first: IntArray, second: IntArray): IntArray {
    val out = mutableListOf<Int>()
    var i = 0; var j = 0
    while (i < first.size && j < second.size) {
        out.add(if (first[i] <= second[j]) first[i++] else second[j++])
    }
    while (i < first.size) out.add(first[i++])
    return out.toIntArray()
}
