// 검증용 정답 (§6.1 solutions/). 이메일 → 처음 본 계정, 다시 보이면 union.
fun mergedAccountsCount(accounts: Array<String>): Int {
    val n = accounts.size
    val parent = IntArray(n) { it }
    fun find(x: Int): Int { var r = x; while (parent[r] != r) { parent[r] = parent[parent[r]]; r = parent[r] }; return r }
    val owner = HashMap<String, Int>()
    for (i in 0 until n) {
        val emails = accounts[i].substringAfter(':').split(',')
        for (email in emails) {
            val seen = owner[email]
            if (seen == null) owner[email] = i
            else { val a = find(seen); val b = find(i); Drill.compare(a, b); if (a != b) parent[a] = b }
        }
    }
    var count = 0
    for (i in 0 until n) if (find(i) == i) count += 1
    Drill.write(0, count)
    return count
}
