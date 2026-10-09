// kind: WRONG_BRANCH
// 금액이 딱 맞는 둘을 먼저 짝짓고, 나머지는 한 줄로 세운다. 나머지가 다시 여러 무리로 쪼개질 수 있다.
fun settleDebts(transactions: IntArray): Int {
    val balance = IntArray(20)
    for (i in transactions.indices step 3) { balance[transactions[i]] -= transactions[i + 2]; balance[transactions[i + 1]] += transactions[i + 2] }
    val rest = balance.filter { it != 0 }.toMutableList()
    var count = 0
    var i = 0
    while (i < rest.size) {
        val j = (i + 1 until rest.size).firstOrNull { rest[it] == -rest[i] }
        if (j != null) { rest.removeAt(j); rest.removeAt(i); count += 1 } else i += 1
    }
    return count + if (rest.isEmpty()) 0 else rest.size - 1
}
