// kind: OFF_BY_ONE
// 행을 바꿔 끼우지 않고 현재 행을 앞 행 위에 덮어쓴다. 대각선 값이 이미 갱신된 것을 읽는다.
fun lcsLength(first: String, second: String): Int {
    val row = IntArray(second.length + 1)
    for (i in 1..first.length) {
        for (j in 1..second.length) {
            row[j] = if (first[i - 1] == second[j - 1]) row[j - 1] + 1 else maxOf(row[j], row[j - 1])
        }
    }
    return row[second.length]
}
