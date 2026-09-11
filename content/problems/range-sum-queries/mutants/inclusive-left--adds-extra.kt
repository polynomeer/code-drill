// kind: OFF_BY_ONE
// 왼쪽 끝 앞의 원소까지 포함한다. prefix[r+1] - prefix[l-1] 을 l=0 에서도 쓴다.
fun rangeSums(nums: IntArray, queries: IntArray): IntArray {
    val prefix = IntArray(nums.size + 2)
    for (i in nums.indices) prefix[i + 2] = prefix[i + 1] + nums[i]
    val out = IntArray(queries.size / 2)
    for (q in out.indices) out[q] = prefix[queries[2 * q + 1] + 2] - prefix[queries[2 * q]]
    return out
}
