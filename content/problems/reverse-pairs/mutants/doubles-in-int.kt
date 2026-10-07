// kind: MISSING_EDGE_CASE
// 2 × nums[j] 를 Int 로 계산한다. 값이 크면 넘쳐 부호가 뒤집힌다.
fun reversePairs(nums: IntArray): Int {
    val a = nums.copyOf()
    fun sort(lo: Int, hi: Int): Int {
        if (hi - lo <= 1) return 0
        val mid = (lo + hi) ushr 1
        var count = sort(lo, mid) + sort(mid, hi)
        var j = mid
        for (i in lo until mid) { while (j < hi && a[i] > 2 * a[j]) j += 1; count += j - mid }
        a.sort(lo, hi)
        return count
    }
    return sort(0, a.size)
}
