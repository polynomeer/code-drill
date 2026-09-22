// kind: PERFORMANCE
// 구간을 하나씩 AND 한다. 20 억 번이다.
fun bitwiseAndOfRange(left: Int, right: Int): Int {
    var result = left
    var i = left
    while (i < right) { i += 1; Drill.compare(i, right); result = result and i; if (result == 0) break }
    return result
}
