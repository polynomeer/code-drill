// 검증용 정답 (§6.1 solutions/). 두 포인터.
fun mergeSorted(first: IntArray, second: IntArray): IntArray {
    val out = IntArray(first.size + second.size)
    var i = 0
    var j = 0
    var k = 0
    while (i < first.size && j < second.size) {
        Drill.compare(i, j)
        out[k] = if (first[i] <= second[j]) first[i++] else second[j++]
        Drill.write(k, out[k])
        k += 1
    }
    while (i < first.size) { out[k] = first[i++]; Drill.write(k, out[k]); k += 1 }
    while (j < second.size) { out[k] = second[j++]; Drill.write(k, out[k]); k += 1 }
    return out
}
