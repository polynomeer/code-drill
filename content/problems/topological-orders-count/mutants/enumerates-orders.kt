// kind: PERFORMANCE
// 순서를 하나씩 만들어 센다. 맞지만, 답만큼 걸린다 — 간선이 없으면 18! 이다.
fun topologicalOrdersCount(n: Int, edges: IntArray): Int {
    val need = IntArray(n)
    for (i in edges.indices step 2) need[edges[i + 1]] = need[edges[i + 1]] or (1 shl edges[i])
    var count = 0L
    fun go(mask: Int) {
        if (mask == (1 shl n) - 1) { count = (count + 1) % 1_000_000_007; return }
        for (v in 0 until n) if (mask shr v and 1 == 0 && need[v] and mask == need[v]) go(mask or (1 shl v))
    }
    go(0)
    return count.toInt()
}
