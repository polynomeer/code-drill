// kind: WRONG_ALGORITHM
// 가장 많이 빚진 사람이 가장 많이 받을 사람에게 갚는 것을 되풀이한다. 탐욕으로는 무리를 찾지 못한다.
fun settleDebts(transactions: IntArray): Int {
    val balance = IntArray(20)
    for (i in transactions.indices step 3) { balance[transactions[i]] -= transactions[i + 2]; balance[transactions[i + 1]] += transactions[i + 2] }
    var count = 0
    while (true) {
        val hi = balance.indices.maxByOrNull { balance[it] }!!
        val lo = balance.indices.minByOrNull { balance[it] }!!
        if (balance[hi] == 0) return count
        val x = minOf(balance[hi], -balance[lo])
        balance[hi] -= x; balance[lo] += x
        count += 1
    }
}
