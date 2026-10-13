// 검증용 정답 (§6.1 solutions/). 간선을 폭이 큰 것부터 유니온 파인드에 넣고, s 와 t 가 처음 이어지는 간선의 폭을 낸다.
fun widestPath(n: Int, edges: IntArray, s: Int, t: Int): Int {
    val m = edges.size / 3
    // (폭, 간선 번호)를 Long 하나에 담아 기본형 배열로 정렬한다 — 상자에 담긴 정수 20 만 개를 정렬하는 것보다 훨씬 가볍다
    val keys = LongArray(m) { (edges[3 * it + 2].toLong() shl 20) or it.toLong() }
    keys.sort()
    val order = IntArray(m) { (keys[m - 1 - it] and 0xFFFFF).toInt() }
    val parent = IntArray(n) { it }
    fun find(x: Int): Int {
        var r = x
        while (parent[r] != r) { parent[r] = parent[parent[r]]; r = parent[r] }
        return r
    }
    for (e in order) {
        val u = edges[3 * e]; val v = edges[3 * e + 1]
        val ru = find(u); val rv = find(v)
        if (ru != rv) { parent[ru] = rv; Drill.edge(u.toString(), v.toString()) }
        if (find(s) == find(t)) return edges[3 * e + 2]
    }
    return -1
}
