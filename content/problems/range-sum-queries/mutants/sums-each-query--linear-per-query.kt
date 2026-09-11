// kind: PERFORMANCE
// 질의마다 구간을 다시 더한다. O(n·q).
fun rangeSums(nums: IntArray, queries: IntArray): IntArray {
    val out = IntArray(queries.size / 2)
    for (q in out.indices) {
        var s = 0
        for (i in queries[2 * q]..queries[2 * q + 1]) { Drill.visit(i, nums[i]); s += nums[i] }
        out[q] = s
    }
    return out
}
