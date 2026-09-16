// kind: PERFORMANCE
// 거리를 이분 탐색하되 쌍을 셀 때 정렬도 두 포인터도 없이 모든 쌍을 본다. O(n² log W).
fun kthPairDistance(nums: IntArray, k: Int): Int {
    val n = nums.size
    fun pairsWithin(limit: Int): Long {
        var count = 0L
        for (i in 0 until n) for (j in i + 1 until n) { Drill.compare(i, j); if (kotlin.math.abs(nums[i] - nums[j]) <= limit) count += 1 }
        return count
    }
    var lo = 0; var hi = nums.max() - nums.min()
    while (lo < hi) { val mid = (lo + hi) / 2; if (pairsWithin(mid) >= k) hi = mid else lo = mid + 1 }
    return lo
}
