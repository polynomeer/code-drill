// kind: WRONG_BRANCH
// 글자가 맞았을 때 앞 행의 대각선이 아니라 같은 행의 왼쪽에 1 을 더한다. 한 글자를 두 번 센다.
fun lcsLength(first: String, second: String): Int {
    var previous = IntArray(second.length + 1)
    var current = IntArray(second.length + 1)
    for (i in 1..first.length) {
        for (j in 1..second.length) {
            current[j] = if (first[i - 1] == second[j - 1]) current[j - 1] + 1 else maxOf(previous[j], current[j - 1])
        }
        val swap = previous; previous = current; current = swap
    }
    return previous[second.length]
}
