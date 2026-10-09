// kind: PERFORMANCE
// 앞사람의 잔액을 부호가 반대인 뒷사람에게 넘겨 보는 백트래킹. 맞지만, 쪼갤 수 없는 잔액 스무 명이면 가지가 폭발한다.
fun settleDebts(transactions: IntArray): Int {
    val balance = IntArray(20)
    for (i in transactions.indices step 3) { balance[transactions[i]] -= transactions[i + 2]; balance[transactions[i + 1]] += transactions[i + 2] }
    val values = balance.filter { it != 0 }.toIntArray()
    fun go(start: Int): Int {
        var i = start
        while (i < values.size && values[i] == 0) i += 1
        if (i == values.size) return 0
        var best = Int.MAX_VALUE
        for (j in i + 1 until values.size) {
            if (values[j].toLong() * values[i] < 0) {
                values[j] += values[i]
                best = minOf(best, 1 + go(i + 1))
                values[j] -= values[i]
            }
        }
        return best
    }
    return go(0)
}
