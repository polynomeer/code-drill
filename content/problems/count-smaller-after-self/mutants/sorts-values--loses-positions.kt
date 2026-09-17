// kind: WRONG_BRANCH
// 값을 정렬하고 자리를 잃는다. 답이 원래 자리가 아니라 정렬된 자리에 적힌다.
fun countSmallerAfter(nums: IntArray): IntArray {
    val n = nums.size
    val values = nums.copyOf()
    val counts = IntArray(n)
    val buffer = IntArray(n)
    val cbuf = IntArray(n)
    fun sort(lo: Int, hi: Int) {
        if (hi - lo <= 1) return
        val mid = (lo + hi) / 2
        sort(lo, mid); sort(mid, hi)
        var i = lo; var j = mid; var out = lo
        while (i < mid || j < hi) {
            if (j >= hi || (i < mid && values[i] <= values[j])) { cbuf[out] = counts[i] + (j - mid); buffer[out++] = values[i++] } else { cbuf[out] = counts[j]; buffer[out++] = values[j++] }
        }
        for (t in lo until hi) { values[t] = buffer[t]; counts[t] = cbuf[t] }
    }
    sort(0, n)
    return counts
}
