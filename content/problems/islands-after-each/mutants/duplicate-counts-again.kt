// kind: MISSING_EDGE_CASE
// 이미 땅인 칸을 새 섬으로 또 센다.
fun islandsAfterEach(rows: Int, cols: Int, positions: IntArray): IntArray {
    val parent = IntArray(rows * cols) { -1 }
    fun find(x: Int): Int { var v = x; while (parent[v] != v) v = parent[v]; return v }
    val out = IntArray(positions.size / 2)
    var count = 0
    for (i in out.indices) {
        val r = positions[2 * i]; val c = positions[2 * i + 1]
        val cell = r * cols + c
        parent[cell] = cell
        count += 1
        for ((dr, dc) in listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)) {
            val nr = r + dr; val nc = c + dc
            if (nr !in 0 until rows || nc !in 0 until cols) continue
            val other = nr * cols + nc
            if (parent[other] == -1) continue
            val a = find(cell); val b = find(other)
            if (a != b) { parent[a] = b; count -= 1 }
        }
        out[i] = count
    }
    return out
}
