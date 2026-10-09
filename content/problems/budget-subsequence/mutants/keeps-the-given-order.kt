// kind: WRONG_ALGORITHM
// 정렬하지 않고 주어진 순서대로 앞에서부터 고른다. 작은 것부터 골라야 가장 많이 고른다.
fun budgetSubsequence(nums: IntArray, queries: IntArray): IntArray {
    val prefix = LongArray(nums.size + 1)
    for (i in nums.indices) prefix[i + 1] = prefix[i] + nums[i]
    return IntArray(queries.size) { j ->
        var low = 0; var high = nums.size
        while (low < high) { val mid = (low + high + 1) / 2; if (prefix[mid] <= queries[j]) low = mid else high = mid - 1 }
        low
    }
}
