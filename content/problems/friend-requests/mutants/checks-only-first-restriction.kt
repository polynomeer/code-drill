// kind: MISSING_EDGE_CASE
// 금지 목록의 첫 쌍만 본다. 뒤에 적힌 금지는 지켜지지 않는다.
fun friendRequests(n: Int, restrictions: IntArray, requests: IntArray): IntArray {
    val parent = IntArray(n) { it }
    fun find(start: Int): Int { var x = start; while (parent[x] != x) { parent[x] = parent[parent[x]]; x = parent[x] }; return x }
    val out = IntArray(requests.size / 2)
    for (q in out.indices) {
        val a = find(requests[2 * q]); val b = find(requests[2 * q + 1])
        var allowed = true
        if (a != b && restrictions.isNotEmpty()) {
            val x = find(restrictions[0]); val y = find(restrictions[1])
            if ((x == a && y == b) || (x == b && y == a)) allowed = false
        }
        if (allowed) parent[a] = b
        out[q] = if (allowed) 1 else 0
    }
    return out
}
