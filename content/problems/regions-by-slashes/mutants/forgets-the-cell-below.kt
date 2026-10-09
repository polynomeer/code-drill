// kind: WRONG_BRANCH
// 오른쪽 칸과만 잇고 아래 칸과는 잇지 않는다. 줄마다 영역이 따로 세어진다.
fun regionsBySlashes(grid: Array<String>): Int {
    val n = grid.size
    val parent = IntArray(4 * n * n) { it }
    fun find(x: Int): Int { var r = x; while (parent[r] != r) { parent[r] = parent[parent[r]]; r = parent[r] }; return r }
    var regions = 4 * n * n
    fun union(a: Int, b: Int) { val ra = find(a); val rb = find(b); if (ra != rb) { parent[ra] = rb; regions -= 1 } }
    for (r in 0 until n) for (c in 0 until n) {
        val base = 4 * (r * n + c)
        when (grid[r][c]) {
            '/' -> { union(base, base + 3); union(base + 1, base + 2) }
            '\\' -> { union(base, base + 1); union(base + 2, base + 3) }
            else -> { union(base, base + 1); union(base + 1, base + 2); union(base + 2, base + 3) }
        }
        if (c + 1 < n) union(base + 1, 4 * (r * n + c + 1) + 3)
    }
    return regions
}
