// kind: OFF_BY_ONE
// 거리가 한도 '미만'인 쌍을 센다. 이하여야 한다 — 답이 정확히 한도인 쌍을 놓친다.
fun kthPairDistance(nums: IntArray, k: Int): Int {
    val values = nums.sortedArray()
    val n = values.size
    fun pairsWithin(limit: Int): Long {
        var count = 0L; var left = 0
        for (right in 0 until n) {
            while (values[right] - values[left] >= limit) left += 1
            count += right - left
        }
        return count
    }
    var lo = 0; var hi = values[n - 1] - values[0]
    while (lo < hi) { val mid = (lo + hi) / 2; if (pairsWithin(mid) >= k) hi = mid else lo = mid + 1 }
    return lo
}
