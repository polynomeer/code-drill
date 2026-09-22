// kind: MISSING_EDGE_CASE
// 이메일의 주인을 볼 때마다 덮어쓰고 합치지는 않는다. 사슬이 끊긴다.
fun mergedAccountsCount(accounts: Array<String>): Int {
    val n = accounts.size
    val parent = IntArray(n) { it }
    fun find(x: Int): Int { var r = x; while (parent[r] != r) { parent[r] = parent[parent[r]]; r = parent[r] }; return r }
    val owner = HashMap<String, Int>()
    for (i in 0 until n) {
        var merged = false
        for (email in accounts[i].substringAfter(':').split(',')) {
            val seen = owner[email]
            if (seen != null && !merged) { val a = find(seen); val b = find(i); if (a != b) parent[a] = b; merged = true }
            owner[email] = i
        }
    }
    var count = 0
    for (i in 0 until n) if (find(i) == i) count += 1
    return count
}
