// kind: WRONG_ALGORITHM
// 합치는 포인터로 함께 센다. 세는 기준(2 배보다 큼)과 합치는 기준(작거나 같음)이 달라 쌍을 놓친다.
fun reversePairs(nums: IntArray): Int {
    val a = nums.copyOf()
    val buffer = IntArray(a.size)
    fun sort(lo: Int, hi: Int): Long {
        if (hi - lo <= 1) return 0
        val mid = (lo + hi) ushr 1
        var count = sort(lo, mid) + sort(mid, hi)
        var l = lo; var r = mid; var k = lo
        while (l < mid || r < hi) {
            if (r >= hi || (l < mid && a[l] <= a[r])) { buffer[k] = a[l]; l += 1 }
            else { if (l < mid && a[l].toLong() > 2L * a[r]) count += mid - l; buffer[k] = a[r]; r += 1 }
            k += 1
        }
        System.arraycopy(buffer, lo, a, lo, hi - lo)
        return count
    }
    return sort(0, a.size).toInt()
}
