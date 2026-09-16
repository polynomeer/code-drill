// kind: MISSING_EDGE_CASE
// 정렬하지 않고 두 포인터를 돌린다. 이미 정렬된 입력에서만 맞는다.
fun kthPairDistance(nums: IntArray, k: Int): Int {
    val values = nums
    val n = values.size
    fun pairsWithin(limit: Int): Long {
        var count = 0L; var left = 0
        for (right in 0 until n) {
            while (left < right && kotlin.math.abs(values[right] - values[left]) > limit) left += 1
            count += right - left
        }
        return count
    }
    var lo = 0; var hi = values.max() - values.min()
    while (lo < hi) { val mid = (lo + hi) / 2; if (pairsWithin(mid) >= k) hi = mid else lo = mid + 1 }
    return lo
}
