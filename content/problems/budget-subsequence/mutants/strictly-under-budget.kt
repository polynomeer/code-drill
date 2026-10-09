// kind: OFF_BY_ONE
// 누적합이 예산보다 작아야 고른다. 예산과 딱 같아도 고를 수 있다.
fun budgetSubsequence(nums: IntArray, queries: IntArray): IntArray {
    val sorted = nums.sortedArray()
    val prefix = LongArray(sorted.size + 1)
    for (i in sorted.indices) prefix[i + 1] = prefix[i] + sorted[i]
    return IntArray(queries.size) { j ->
        var low = 0; var high = sorted.size
        while (low < high) { val mid = (low + high + 1) / 2; if (prefix[mid] < queries[j]) low = mid else high = mid - 1 }
        low
    }
}
