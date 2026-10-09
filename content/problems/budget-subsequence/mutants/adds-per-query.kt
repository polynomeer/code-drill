// kind: PERFORMANCE
// 예산마다 정렬된 배열을 처음부터 더해 간다. 맞지만, 예산이 크면 예산 수 × 원소 수다.
fun budgetSubsequence(nums: IntArray, queries: IntArray): IntArray {
    val sorted = nums.sortedArray()
    return IntArray(queries.size) { j ->
        var total = 0L
        var count = 0
        for (v in sorted) { if (total + v > queries[j]) break; total += v; count += 1 }
        count
    }
}
