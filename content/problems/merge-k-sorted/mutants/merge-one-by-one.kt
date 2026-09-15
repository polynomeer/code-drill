// kind: PERFORMANCE
// 목록을 하나씩 차례로 합친다. 앞의 결과를 매번 다시 훑어 O(N·k).
fun mergeSorted(sizes: IntArray, values: IntArray): IntArray {
    var acc = IntArray(0)
    var at = 0
    for (size in sizes) {
        val list = values.copyOfRange(at, at + size); at += size
        val merged = IntArray(acc.size + list.size)
        var i = 0; var j = 0; var n = 0
        while (i < acc.size || j < list.size) {
            Drill.compare(i, j)
            merged[n++] = if (j >= list.size || (i < acc.size && acc[i] <= list[j])) acc[i++] else list[j++]
        }
        acc = merged
    }
    return acc
}
