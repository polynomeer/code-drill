// kind: WRONG_ALGORITHM
// 잔액이 0 아닌 사람을 한 줄로 세워 k − 1 번이라고 한다. 합이 0 인 무리로 쪼개지면 무리마다 한 번씩 아낀다.
fun settleDebts(transactions: IntArray): Int {
    val balance = IntArray(20)
    for (i in transactions.indices step 3) { balance[transactions[i]] -= transactions[i + 2]; balance[transactions[i + 1]] += transactions[i + 2] }
    val k = balance.count { it != 0 }
    return if (k == 0) 0 else k - 1
}
