// kind: PERFORMANCE
// 칸마다 블록을 전부 더한다. O(rows·cols·k²).
fun blockSum(grid: Array<IntArray>, k: Int): Array<IntArray> {
    val rows = grid.size; val cols = grid[0].size
    return Array(rows) { r ->
        IntArray(cols) { c ->
            var total = 0
            for (rr in maxOf(0, r - k) until minOf(rows, r + k + 1))
                for (cc in maxOf(0, c - k) until minOf(cols, c + k + 1)) { Drill.visit(rr, cc); total += grid[rr][cc] }
            total
        }
    }
}
