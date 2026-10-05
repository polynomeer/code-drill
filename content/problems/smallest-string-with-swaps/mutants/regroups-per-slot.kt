// kind: PERFORMANCE
// 자리마다 그 자리가 속한 무리를 처음부터 다시 찾는다. 무리가 크면 자리 수의 제곱이 된다.
fun smallestStringWithSwaps(s: String, pairs: IntArray): String {
    val n = s.length
    val adj = Array(n) { ArrayList<Int>() }
    for (i in pairs.indices step 2) { adj[pairs[i]].add(pairs[i + 1]); adj[pairs[i + 1]].add(pairs[i]) }
    val out = CharArray(n)
    val mark = IntArray(n) { -1 }
    for (i in 0 until n) {
        val slots = ArrayList<Int>()
        val stack = java.util.ArrayDeque<Int>()
        stack.push(i); mark[i] = i
        while (stack.isNotEmpty()) {
            val v = stack.pop(); slots.add(v)
            for (u in adj[v]) if (mark[u] != i) { mark[u] = i; stack.push(u) }
        }
        slots.sort()
        val letters = slots.map { s[it] }.sorted()
        out[i] = letters[slots.binarySearch(i)]
    }
    return String(out)
}
