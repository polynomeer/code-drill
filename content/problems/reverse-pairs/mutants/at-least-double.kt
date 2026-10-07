// kind: OFF_BY_ONE
// nums[i] 가 2 × nums[j] 와 같아도 센다. 엄격히 커야 한다.
fun reversePairs(nums: IntArray): Int {
    val a = nums.copyOf()
    fun sort(lo: Int, hi: Int): Long {
        if (hi - lo <= 1) return 0
        val mid = (lo + hi) ushr 1
        var count = sort(lo, mid) + sort(mid, hi)
        var j = mid
        for (i in lo until mid) { while (j < hi && a[i].toLong() >= 2L * a[j]) j += 1; count += j - mid }
        a.sort(lo, hi)
        return count
    }
    return sort(0, a.size).toInt()
}
