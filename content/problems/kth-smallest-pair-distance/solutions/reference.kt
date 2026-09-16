// 검증용 정답 (§6.1 solutions/). 거리를 이분 탐색하고, 거리마다 두 포인터로 쌍을 센다.
fun kthPairDistance(nums: IntArray, k: Int): Int {
    val values = nums.sortedArray()
    val n = values.size
    fun pairsWithin(limit: Int): Long {
        var count = 0L
        var left = 0
        for (right in 0 until n) {
            while (values[right] - values[left] > limit) { left += 1; Drill.pointer("left", left) }
            count += right - left
        }
        return count
    }
    var lo = 0
    var hi = values[n - 1] - values[0]
    while (lo < hi) {
        val mid = (lo + hi) / 2
        Drill.compare(lo, hi)
        if (pairsWithin(mid) >= k) hi = mid else lo = mid + 1
    }
    return lo
}
