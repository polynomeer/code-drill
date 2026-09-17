// kind: WRONG_ALGORITHM
// 앞에 있는 더 작은 수를 센다.
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
            if (i >= mid || (j < hi && nums[order[j]] < nums[order[i]])) { counts[order[j]] += i - lo; buffer[out++] = order[j++] } else buffer[out++] = order[i++]
        }
        for (t in lo until hi) order[t] = buffer[t]
    }
    sort(0, n)
    return counts
}
