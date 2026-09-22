// kind: PERFORMANCE
// 계정 쌍마다 이메일이 겹치는지 본다. O(n² × 이메일).
fun mergedAccountsCount(accounts: Array<String>): Int {
    val n = accounts.size
    val emails = accounts.map { it.substringAfter(':').split(',').toHashSet() }
    val parent = IntArray(n) { it }
    fun find(x: Int): Int { var r = x; while (parent[r] != r) { parent[r] = parent[parent[r]]; r = parent[r] }; return r }
    for (i in 0 until n) for (j in i + 1 until n) {
        Drill.compare(i, j)
        if (emails[i].any { it in emails[j] }) { val a = find(i); val b = find(j); if (a != b) parent[a] = b }
    }
    var count = 0
    for (i in 0 until n) if (find(i) == i) count += 1
    return count
}
