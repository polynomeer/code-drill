// kind: WRONG_ALGORITHM
// 두 정점을 바로 잇는 간선만 본다. 여러 간선을 이어 가는 길도 길이다.
fun limitedPathQueries(n: Int, edges: IntArray, queries: IntArray): IntArray {
    val lightest = HashMap<Long, Int>()
    for (e in edges.indices step 3) {
        val a = minOf(edges[e], edges[e + 1]).toLong(); val b = maxOf(edges[e], edges[e + 1]).toLong()
        val key = a * 1_000_000 + b
        lightest[key] = minOf(lightest[key] ?: Int.MAX_VALUE, edges[e + 2])
    }
    val q = queries.size / 3
    return IntArray(q) { i ->
        val p = queries[3 * i]; val t = queries[3 * i + 1]
        if (p == t) 1 else {
            val w = lightest[minOf(p, t).toLong() * 1_000_000 + maxOf(p, t)]
            if (w != null && w < queries[3 * i + 2]) 1 else 0
        }
    }
}
