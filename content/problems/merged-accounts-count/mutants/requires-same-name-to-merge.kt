// kind: WRONG_BRANCH
// 이메일이 같아도 이름이 다르면 합치지 않는다.
fun mergedAccountsCount(accounts: Array<String>): Int {
    val n = accounts.size
    val parent = IntArray(n) { it }
    fun find(x: Int): Int { var r = x; while (parent[r] != r) { parent[r] = parent[parent[r]]; r = parent[r] }; return r }
    val owner = HashMap<String, Int>()
    for (i in 0 until n) {
        val name = accounts[i].substringBefore(':')
        for (email in accounts[i].substringAfter(':').split(',')) {
            val seen = owner[email]
            if (seen == null) owner[email] = i
            else if (accounts[seen].substringBefore(':') == name) { val a = find(seen); val b = find(i); if (a != b) parent[a] = b }
        }
    }
    var count = 0
    for (i in 0 until n) if (find(i) == i) count += 1
    return count
}
