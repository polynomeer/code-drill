// 검증용 정답 (§6.1 solutions/). 정렬해 Long 누적합을 만들고, 예산마다 누적합이 예산 이하인 마지막 자리를 이분 탐색한다.
fun budgetSubsequence(nums: IntArray, queries: IntArray): IntArray {
    val sorted = nums.sortedArray()
    val prefix = LongArray(sorted.size + 1)
    for (i in sorted.indices) prefix[i + 1] = prefix[i] + sorted[i]
    return IntArray(queries.size) { j ->
        var low = 0
        var high = sorted.size
        while (low < high) {
            val mid = (low + high + 1) / 2
            if (prefix[mid] <= queries[j]) low = mid else high = mid - 1
        }
        Drill.pointer("answer", low)
        low
    }
}
