// kind: PERFORMANCE
// 두 사람의 자리를 기억하지 않고 재귀한다. 4^(2n) 이다.
fun cherryPickup(grid: Array<IntArray>): Int {
    val n = grid.size
    val neg = Int.MIN_VALUE / 2
    fun go(r1: Int, c1: Int, r2: Int, c2: Int): Int {
        if (r1 >= n || c1 >= n || r2 >= n || c2 >= n || grid[r1][c1] < 0 || grid[r2][c2] < 0) return neg
        if (r1 == n - 1 && c1 == n - 1) return grid[r1][c1]
        Drill.compare(r1, r2)
        var best = neg
        for (d1 in 0..1) for (d2 in 0..1) {
            val v = go(r1 + d1, c1 + 1 - d1, r2 + d2, c2 + 1 - d2)
            if (v > best) best = v
        }
        val gain = grid[r1][c1] + (if (r1 != r2 || c1 != c2) grid[r2][c2] else 0)
        return if (best == neg) neg else best + gain
    }
    return maxOf(0, go(0, 0, 0, 0))
}
