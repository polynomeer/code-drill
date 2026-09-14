// kind: OFF_BY_ONE
// 행의 끝과 다음 행의 처음을 이웃으로 본다. 칸 번호 ±1 만 보고 열 경계를 잊었다.
fun islandsAfterEach(rows: Int, cols: Int, positions: IntArray): IntArray {
    val total = rows * cols
    val parent = IntArray(total) { -1 }
    fun find(x: Int): Int { var v = x; while (parent[v] != v) v = parent[v]; return v }
    val out = IntArray(positions.size / 2)
    var count = 0
    for (i in out.indices) {
        val cell = positions[2 * i] * cols + positions[2 * i + 1]
        if (parent[cell] == -1) {
            parent[cell] = cell
            count += 1
            for (other in intArrayOf(cell - 1, cell + 1, cell - cols, cell + cols)) {
                if (other < 0 || other >= total || parent[other] == -1) continue
                val a = find(cell); val b = find(other)
                if (a != b) { parent[a] = b; count -= 1 }
            }
        }
        out[i] = count
    }
    return out
}
