// kind: MISSING_EDGE_CASE
// 먼저 합치고 나서 금지가 깨졌는지 본다. 거절된 요청이 무리를 바꿔 놓는다.
fun friendRequests(n: Int, restrictions: IntArray, requests: IntArray): IntArray {
    val parent = IntArray(n) { it }
    fun find(start: Int): Int { var x = start; while (parent[x] != x) { parent[x] = parent[parent[x]]; x = parent[x] }; return x }
    val out = IntArray(requests.size / 2)
    for (q in out.indices) {
        val a = find(requests[2 * q]); val b = find(requests[2 * q + 1])
        if (a != b) parent[a] = b
        var allowed = true
        var j = 0
        while (j < restrictions.size) {
            if (find(restrictions[j]) == find(restrictions[j + 1])) { allowed = false; break }
            j += 2
        }
        out[q] = if (allowed) 1 else 0
    }
    return out
}
