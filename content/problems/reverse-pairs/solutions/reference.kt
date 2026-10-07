// 검증용 정답 (§6.1 solutions/). 병합 정렬 — 정렬된 두 반에서 먼저 세고, 그다음 합친다.
fun reversePairs(nums: IntArray): Int {
    val a = nums.copyOf()
    val buffer = IntArray(a.size)
    fun sort(lo: Int, hi: Int): Long {
        if (hi - lo <= 1) return 0
        val mid = (lo + hi) ushr 1
        var count = sort(lo, mid) + sort(mid, hi)
        var j = mid
        for (i in lo until mid) {
            while (j < hi && a[i].toLong() > 2L * a[j]) j += 1
            Drill.compare(i, j)
            count += j - mid
        }
        var l = lo; var r = mid; var k = lo
        while (l < mid || r < hi) {
            if (r >= hi || (l < mid && a[l] <= a[r])) { buffer[k] = a[l]; l += 1 } else { buffer[k] = a[r]; r += 1 }
            k += 1
        }
        System.arraycopy(buffer, lo, a, lo, hi - lo)
        return count
    }
    return sort(0, a.size).toInt()
}
