// kind: WRONG_ALGORITHM
// 요청한 두 사람이 곧바로 금지된 쌍인지만 본다. 무리를 타고 이어지는 금지를 놓친다.
fun friendRequests(n: Int, restrictions: IntArray, requests: IntArray): IntArray {
    val parent = IntArray(n) { it }
    fun find(start: Int): Int { var x = start; while (parent[x] != x) { parent[x] = parent[parent[x]]; x = parent[x] }; return x }
    val out = IntArray(requests.size / 2)
    for (q in out.indices) {
        val u = requests[2 * q]; val v = requests[2 * q + 1]
        var allowed = true
        var j = 0
        while (j < restrictions.size) {
            if ((restrictions[j] == u && restrictions[j + 1] == v) || (restrictions[j] == v && restrictions[j + 1] == u)) { allowed = false; break }
            j += 2
        }
        if (allowed) parent[find(u)] = find(v)
        out[q] = if (allowed) 1 else 0
    }
    return out
}
