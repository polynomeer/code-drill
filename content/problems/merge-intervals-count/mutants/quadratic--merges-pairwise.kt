// kind: PERFORMANCE
// 구간마다 다른 모든 구간과 겹치는지 본다. O(n²).
fun mergeCount(intervals: IntArray): Int {
    val n = intervals.size / 2
    val parent = IntArray(n) { it }
    fun find(x: Int): Int { var v = x; while (parent[v] != v) v = parent[v]; return v }
    for (i in 0 until n) for (j in i + 1 until n) {
        Drill.compare(i, j)
        val overlap = intervals[2 * i] <= intervals[2 * j + 1] && intervals[2 * j] <= intervals[2 * i + 1]
        if (overlap) { val a = find(i); val b = find(j); if (a != b) parent[b] = a }
    }
    return (0 until n).count { find(it) == it }
}
