// kind: MISSING_EDGE_CASE
// 누적합을 Int 로 더한다. 20 억을 넘으면 음수로 감겨 누적합이 더는 늘기만 하지 않는다.
fun budgetSubsequence(nums: IntArray, queries: IntArray): IntArray {
    val sorted = nums.sortedArray()
    val prefix = IntArray(sorted.size + 1)
    for (i in sorted.indices) prefix[i + 1] = prefix[i] + sorted[i]
    return IntArray(queries.size) { j ->
        var low = 0; var high = sorted.size
        while (low < high) { val mid = (low + high + 1) / 2; if (prefix[mid] <= queries[j]) low = mid else high = mid - 1 }
        low
    }
}
