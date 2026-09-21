// kind: PERFORMANCE
// 칸마다 밖으로 나가는 가장 낮은 고개를 따로 찾는다. O((RC)²).
fun trappingRainWater2d(heights: Array<IntArray>): Int {
    val rows = heights.size; val cols = heights[0].size
    if (rows < 3 || cols < 3) return 0
    var water = 0L
    val dr = intArrayOf(1, -1, 0, 0); val dc = intArrayOf(0, 0, 1, -1)
    for (sr in 1 until rows - 1) for (sc in 1 until cols - 1) {
        // 이 칸에서 밖으로 나가는 길들의 최댓값의 최솟값 — 수위를 낮은 것부터 올려 가며 닿는지 본다.
        val heap = java.util.PriorityQueue<IntArray>(compareBy { it[0] })
        val seen = BooleanArray(rows * cols)
        heap.add(intArrayOf(heights[sr][sc], sr, sc)); seen[sr * cols + sc] = true
        var pass = 0
        while (heap.isNotEmpty()) {
            val top = heap.poll(); val level = maxOf(pass, top[0]); val r = top[1]; val c = top[2]
            Drill.compare(r, c)
            pass = level
            if (r == 0 || c == 0 || r == rows - 1 || c == cols - 1) break
            for (k in 0 until 4) {
                val nr = r + dr[k]; val nc = c + dc[k]
                if (nr !in 0 until rows || nc !in 0 until cols || seen[nr * cols + nc]) continue
                seen[nr * cols + nc] = true; heap.add(intArrayOf(heights[nr][nc], nr, nc))
            }
        }
        water += maxOf(0, pass - heights[sr][sc])
    }
    return water.toInt()
}
