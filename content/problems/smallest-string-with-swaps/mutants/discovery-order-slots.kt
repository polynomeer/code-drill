// kind: WRONG_BRANCH
// 글자는 정렬하지만 자리는 무리를 훑은 순서대로 채운다. 가장 앞선 글자는 가장 앞 자리에 가야 한다.
fun smallestStringWithSwaps(s: String, pairs: IntArray): String {
    val n = s.length
    val adj = Array(n) { ArrayList<Int>() }
    for (i in pairs.indices step 2) { adj[pairs[i]].add(pairs[i + 1]); adj[pairs[i + 1]].add(pairs[i]) }
    val seen = BooleanArray(n)
    val out = CharArray(n)
    for (start in 0 until n) {
        if (seen[start]) continue
        val slots = ArrayList<Int>()
        val stack = java.util.ArrayDeque<Int>()
        stack.push(start); seen[start] = true
        while (stack.isNotEmpty()) {
            val v = stack.pop(); slots.add(v)
            for (u in adj[v]) if (!seen[u]) { seen[u] = true; stack.push(u) }
        }
        val letters = slots.map { s[it] }.sorted()
        for (k in slots.indices) out[slots[k]] = letters[k]
    }
    return String(out)
}
