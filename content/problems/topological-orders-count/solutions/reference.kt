// 검증용 정답 (§6.1 solutions/). 놓은 정점의 집합마다 방법의 수 — 앞선 정점이 모두 놓인 정점을 다음에 놓는다.
fun topologicalOrdersCount(n: Int, edges: IntArray): Int {
    val mod = 1_000_000_007L
    val need = IntArray(n)
    for (i in edges.indices step 2) need[edges[i + 1]] = need[edges[i + 1]] or (1 shl edges[i])
    val ways = LongArray(1 shl n)
    ways[0] = 1
    for (mask in 0 until (1 shl n)) {
        if (ways[mask] == 0L) continue
        for (v in 0 until n) {
            if (mask shr v and 1 == 0 && need[v] and mask == need[v]) {
                val next = mask or (1 shl v)
                ways[next] = (ways[next] + ways[mask]) % mod
            }
        }
    }
    Drill.write(0, ways[(1 shl n) - 1].toInt())
    return ways[(1 shl n) - 1].toInt()
}
