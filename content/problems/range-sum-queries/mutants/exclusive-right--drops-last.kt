// kind: OFF_BY_ONE
// 오른쪽 끝을 빼고 더한다. prefix[r] - prefix[l] 이다.
fun rangeSums(nums: IntArray, queries: IntArray): IntArray {
    val prefix = IntArray(nums.size + 1)
    for (i in nums.indices) prefix[i + 1] = prefix[i] + nums[i]
    val out = IntArray(queries.size / 2)
    for (q in out.indices) out[q] = prefix[queries[2 * q + 1]] - prefix[queries[2 * q]]
    return out
}
