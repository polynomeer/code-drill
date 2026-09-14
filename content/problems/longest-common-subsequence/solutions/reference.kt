// 검증용 정답 (§6.1 solutions/). 행 두 개만 든다.
fun lcsLength(first: String, second: String): Int {
    var previous = IntArray(second.length + 1)
    var current = IntArray(second.length + 1)
    for (i in 1..first.length) {
        val a = first[i - 1]
        for (j in 1..second.length) {
            current[j] = if (a == second[j - 1]) previous[j - 1] + 1 else maxOf(previous[j], current[j - 1])
        }
        Drill.write(i, current[second.length])
        val swap = previous
        previous = current
        current = swap
    }
    return previous[second.length]
}
