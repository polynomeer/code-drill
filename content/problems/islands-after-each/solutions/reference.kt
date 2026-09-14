// 검증용 정답 (§6.1 solutions/). 경로 압축 유니온 파인드.
fun islandsAfterEach(rows: Int, cols: Int, positions: IntArray): IntArray {
    val parent = IntArray(rows * cols) { -1 }
    fun find(x: Int): Int {
        var v = x
        while (parent[v] != v) { parent[v] = parent[parent[v]]; v = parent[v] }
        return v
    }
    val out = IntArray(positions.size / 2)
    var count = 0
    val dr = intArrayOf(1, -1, 0, 0)
    val dc = intArrayOf(0, 0, 1, -1)
    for (i in out.indices) {
        val r = positions[2 * i]
        val c = positions[2 * i + 1]
        val cell = r * cols + c
        if (parent[cell] == -1) {
            parent[cell] = cell
            count += 1
            for (k in 0 until 4) {
                val nr = r + dr[k]
                val nc = c + dc[k]
                if (nr !in 0 until rows || nc !in 0 until cols) continue
                val other = nr * cols + nc
                if (parent[other] == -1) continue
                val a = find(cell)
                val b = find(other)
                if (a != b) { parent[a] = b; count -= 1; Drill.match(a, b) }
            }
        }
        out[i] = count
        Drill.write(cell, count)
    }
    return out
}
