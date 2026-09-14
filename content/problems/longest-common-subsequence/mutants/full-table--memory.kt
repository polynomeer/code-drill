// kind: PERFORMANCE
// 표 전체를 든다. 시간은 넉넉한데 15,000 × 15,000 의 정수 표는 메모리 한도의 세 배다.
fun lcsLength(first: String, second: String): Int {
    val table = Array(first.length + 1) { IntArray(second.length + 1) }
    for (i in 1..first.length) {
        for (j in 1..second.length) {
            table[i][j] = if (first[i - 1] == second[j - 1]) table[i - 1][j - 1] + 1 else maxOf(table[i - 1][j], table[i][j - 1])
        }
    }
    return table[first.length][second.length]
}
