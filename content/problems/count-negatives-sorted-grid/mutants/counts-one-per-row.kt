// kind: WRONG_BRANCH
// 행마다 음수가 있는지만 보고 하나로 센다.
fun countNegatives(grid: Array<IntArray>): Int {
    var count = 0
    for (row in grid) {
        for (c in row.indices.reversed()) { if (row[c] < 0) { count += 1; break } }
    }
    return count
}
