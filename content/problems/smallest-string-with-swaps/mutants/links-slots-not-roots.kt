// kind: WRONG_BRANCH
// 뿌리가 아니라 자리 자체를 잇는다. 이미 다른 자리에 붙어 있던 자리의 옛 연결이 끊긴다.
fun smallestStringWithSwaps(s: String, pairs: IntArray): String {
    val n = s.length
    val parent = IntArray(n) { it }
    fun find(x: Int): Int { var r = x; while (parent[r] != r) r = parent[r]; return r }
    for (i in pairs.indices step 2) if (find(pairs[i]) != find(pairs[i + 1])) parent[pairs[i]] = pairs[i + 1]
    val counts = HashMap<Int, IntArray>()
    for (i in 0 until n) counts.getOrPut(find(i)) { IntArray(26) }[s[i] - 'a'] += 1
    val out = CharArray(n)
    for (i in 0 until n) {
        val c = counts.getValue(find(i))
        var k = 0
        while (c[k] == 0) k += 1
        c[k] -= 1
        out[i] = 'a' + k
    }
    return String(out)
}
