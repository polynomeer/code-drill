// kind: WRONG_ALGORITHM
// 이름이 같으면 합친다. 동명이인이 한 사람이 된다.
fun mergedAccountsCount(accounts: Array<String>): Int {
    val n = accounts.size
    val parent = IntArray(n) { it }
    fun find(x: Int): Int { var r = x; while (parent[r] != r) { parent[r] = parent[parent[r]]; r = parent[r] }; return r }
    val owner = HashMap<String, Int>()
    for (i in 0 until n) {
        val name = accounts[i].substringBefore(':')
        val seen = owner[name]
        if (seen == null) owner[name] = i else { val a = find(seen); val b = find(i); if (a != b) parent[a] = b }
        for (email in accounts[i].substringAfter(':').split(',')) { val s = owner[email]; if (s == null) owner[email] = i else { val a = find(s); val b = find(i); if (a != b) parent[a] = b } }
    }
    var count = 0
    for (i in 0 until n) if (find(i) == i) count += 1
    return count
}
