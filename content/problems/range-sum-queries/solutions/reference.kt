// 검증용 정답 (§6.1 solutions/). 누적합.
fun rangeSums(nums: IntArray, queries: IntArray): IntArray {
    val prefix = IntArray(nums.size + 1)
    for (i in nums.indices) {
        prefix[i + 1] = prefix[i] + nums[i]
        Drill.write(i + 1, prefix[i + 1])
    }
    val out = IntArray(queries.size / 2)
    for (q in out.indices) {
        val l = queries[2 * q]; val r = queries[2 * q + 1]
        Drill.compare(l, r)
        out[q] = prefix[r + 1] - prefix[l]
    }
    return out
}
