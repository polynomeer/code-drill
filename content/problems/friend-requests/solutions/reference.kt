// 검증용 정답 (§6.1 solutions/). 합치기 전에 금지 목록을 훑어 "합치면 깨지는가"를 본다.
fun friendRequests(n: Int, restrictions: IntArray, requests: IntArray): IntArray {
    val parent = IntArray(n) { it }
    fun find(start: Int): Int {
        var x = start
        while (parent[x] != x) { parent[x] = parent[parent[x]]; x = parent[x] }
        return x
    }
    val out = IntArray(requests.size / 2)
    for (q in out.indices) {
        val a = find(requests[2 * q])
        val b = find(requests[2 * q + 1])
        var allowed = true
        if (a != b) {
            var j = 0
            while (j < restrictions.size) {
                val x = find(restrictions[j])
                val y = find(restrictions[j + 1])
                if ((x == a && y == b) || (x == b && y == a)) { allowed = false; break }
                j += 2
            }
        }
        if (allowed) { parent[a] = b; Drill.match(a, b) }
        out[q] = if (allowed) 1 else 0
        Drill.write(q, out[q])
    }
    return out
}
