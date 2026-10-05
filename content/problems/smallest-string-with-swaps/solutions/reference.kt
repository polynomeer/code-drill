// 검증용 정답 (§6.1 solutions/). 무리를 유니온 파인드로 묶고, 무리마다 글자를 세어 자리 순서대로 채운다.
fun smallestStringWithSwaps(s: String, pairs: IntArray): String {
    val n = s.length
    val parent = IntArray(n) { it }
    fun find(x: Int): Int {
        var r = x
        while (parent[r] != r) { parent[r] = parent[parent[r]]; r = parent[r] }
        return r
    }
    for (i in pairs.indices step 2) {
        val a = find(pairs[i]); val b = find(pairs[i + 1])
        if (a != b) {
            parent[a] = b
            Drill.edge(a.toString(), b.toString())
        }
    }
    val counts = HashMap<Int, IntArray>()
    for (i in 0 until n) counts.getOrPut(find(i)) { IntArray(26) }[s[i] - 'a'] += 1
    val out = CharArray(n)
    for (i in 0 until n) {
        val c = counts.getValue(find(i))
        var k = 0
        while (c[k] == 0) k += 1
        c[k] -= 1
        out[i] = 'a' + k
        Drill.write(i, k)
    }
    return String(out)
}
