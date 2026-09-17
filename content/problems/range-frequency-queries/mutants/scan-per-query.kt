// kind: PERFORMANCE
// 질의마다 구간을 훑는다. O(질의 × 구간 길이).
fun rangeFrequency(nums: IntArray, queries: IntArray): IntArray {
    val out = IntArray(queries.size / 3)
    for (q in out.indices) {
        var c = 0
        for (i in queries[q * 3]..queries[q * 3 + 1]) { Drill.compare(i, q); if (nums[i] == queries[q * 3 + 2]) c += 1 }
        out[q] = c
    }
    return out
}
