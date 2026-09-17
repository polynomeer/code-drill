// kind: OFF_BY_ONE
// 병합에서 같은 값일 때 오른쪽을 먼저 내보낸다. 같은 값이 '더 작은 수'로 세어진다.
fun countSmallerAfter(nums: IntArray): IntArray {
    val n = nums.size
    val counts = IntArray(n)
    val order = IntArray(n) { it }
    val buffer = IntArray(n)
    fun sort(lo: Int, hi: Int) {
        if (hi - lo <= 1) return
        val mid = (lo + hi) / 2
        sort(lo, mid); sort(mid, hi)
        var i = lo; var j = mid; var out = lo
        while (i < mid || j < hi) {
            if (j >= hi || (i < mid && nums[order[i]] < nums[order[j]])) { counts[order[i]] += j - mid; buffer[out++] = order[i++] } else buffer[out++] = order[j++]
        }
        for (t in lo until hi) order[t] = buffer[t]
    }
    sort(0, n)
    return counts
}
