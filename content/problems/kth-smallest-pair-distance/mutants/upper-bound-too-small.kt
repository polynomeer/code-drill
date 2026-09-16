// kind: WRONG_BRANCH
// 이분 탐색의 위 경계를 최댓값의 절반으로 둔다. 답이 그 위에 있으면 경계에서 멈춘다.
fun kthPairDistance(nums: IntArray, k: Int): Int {
    val values = nums.sortedArray()
    val n = values.size
    fun pairsWithin(limit: Int): Long {
        var count = 0L; var left = 0
        for (right in 0 until n) {
            while (values[right] - values[left] > limit) left += 1
            count += right - left
        }
        return count
    }
    var lo = 0; var hi = values[n - 1] / 2
    while (lo < hi) { val mid = (lo + hi) / 2; if (pairsWithin(mid) >= k) hi = mid else lo = mid + 1 }
    return lo
}
