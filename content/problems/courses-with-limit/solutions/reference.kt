// 검증용 정답 (§6.1 solutions/). 들은 과목의 집합마다 최소 학기 — 들을 수 있는 과목이 k 개를 넘으면 크기 k 의 부분집합마다 넘어간다.
fun coursesWithLimit(n: Int, edges: IntArray, k: Int): Int {
    val need = IntArray(n)
    for (i in edges.indices step 2) need[edges[i + 1]] = need[edges[i + 1]] or (1 shl edges[i])
    val full = (1 shl n) - 1
    val inf = Int.MAX_VALUE
    val best = IntArray(1 shl n) { inf }
    best[0] = 0
    for (mask in 0..full) {
        if (best[mask] == inf) continue
        var ready = 0
        for (v in 0 until n) if (mask shr v and 1 == 0 && need[v] and mask == need[v]) ready = ready or (1 shl v)
        if (Integer.bitCount(ready) <= k) {
            val next = mask or ready
            if (best[mask] + 1 < best[next]) best[next] = best[mask] + 1
            continue
        }
        var sub = ready
        while (sub != 0) {
            if (Integer.bitCount(sub) == k && best[mask] + 1 < best[mask or sub]) best[mask or sub] = best[mask] + 1
            sub = (sub - 1) and ready
        }
    }
    Drill.write(full, best[full])
    return best[full]
}
