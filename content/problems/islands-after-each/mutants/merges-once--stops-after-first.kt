// kind: WRONG_BRANCH
// 이웃 섬을 하나만 합치고 멈춘다. 새 칸이 셋 이상을 잇는 자리에서 틀린다.
fun islandsAfterEach(rows: Int, cols: Int, positions: IntArray): IntArray {
    val parent = IntArray(rows * cols) { -1 }
    fun find(x: Int): Int { var v = x; while (parent[v] != v) v = parent[v]; return v }
    val out = IntArray(positions.size / 2)
    var count = 0
    for (i in out.indices) {
        val r = positions[2 * i]; val c = positions[2 * i + 1]
        val cell = r * cols + c
        if (parent[cell] == -1) {
            parent[cell] = cell
            count += 1
            for ((dr, dc) in listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)) {
                val nr = r + dr; val nc = c + dc
                if (nr !in 0 until rows || nc !in 0 until cols) continue
                val other = nr * cols + nc
                if (parent[other] == -1) continue
                val a = find(cell); val b = find(other)
                if (a != b) { parent[a] = b; count -= 1; break }
            }
        }
        out[i] = count
    }
    return out
}
