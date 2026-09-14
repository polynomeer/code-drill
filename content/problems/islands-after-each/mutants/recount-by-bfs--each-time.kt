// kind: PERFORMANCE
// 땅을 더할 때마다 격자 전체를 BFS 로 다시 센다. O(추가 × 격자).
fun islandsAfterEach(rows: Int, cols: Int, positions: IntArray): IntArray {
    val land = BooleanArray(rows * cols)
    val out = IntArray(positions.size / 2)
    val seen = BooleanArray(rows * cols)
    for (i in out.indices) {
        land[positions[2 * i] * cols + positions[2 * i + 1]] = true
        java.util.Arrays.fill(seen, false)
        var count = 0
        for (start in 0 until rows * cols) {
            if (!land[start] || seen[start]) continue
            count += 1
            val stack = ArrayDeque<Int>()
            stack.addLast(start); seen[start] = true
            while (stack.isNotEmpty()) {
                val cell = stack.removeLast()
                Drill.visit(cell, count)
                val r = cell / cols; val c = cell % cols
                for ((dr, dc) in listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)) {
                    val nr = r + dr; val nc = c + dc
                    if (nr !in 0 until rows || nc !in 0 until cols) continue
                    val other = nr * cols + nc
                    if (land[other] && !seen[other]) { seen[other] = true; stack.addLast(other) }
                }
            }
        }
        out[i] = count
    }
    return out
}
