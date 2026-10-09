// kind: WRONG_BRANCH
// 역빗금을 빗금처럼 잇는다. 두 대각선이 잇는 삼각형 쌍은 서로 다르다.
fun regionsBySlashes(grid: Array<String>): Int {
    val n = grid.size
    val parent = IntArray(4 * n * n) { it }
    fun find(x: Int): Int { var r = x; while (parent[r] != r) { parent[r] = parent[parent[r]]; r = parent[r] }; return r }
    var regions = 4 * n * n
    fun union(a: Int, b: Int) { val ra = find(a); val rb = find(b); if (ra != rb) { parent[ra] = rb; regions -= 1 } }
    for (r in 0 until n) for (c in 0 until n) {
        val base = 4 * (r * n + c)
        if (grid[r][c] != ' ') { union(base, base + 3); union(base + 1, base + 2) }
        else { union(base, base + 1); union(base + 1, base + 2); union(base + 2, base + 3) }
        if (c + 1 < n) union(base + 1, 4 * (r * n + c + 1) + 3)
        if (r + 1 < n) union(base + 2, 4 * ((r + 1) * n + c))
    }
    return regions
}
