// 검증용 정답 (§6.1 solutions/). 자리를 병합 정렬하며 왼쪽이 나갈 때 오른쪽에서 먼저 나간 수를 더한다.
fun countSmallerAfter(nums: IntArray): IntArray {
    val n = nums.size
    val counts = IntArray(n)
    var order = IntArray(n) { it }
    var buffer = IntArray(n)
    fun sort(lo: Int, hi: Int) {
        if (hi - lo <= 1) return
        val mid = (lo + hi) / 2
        sort(lo, mid); sort(mid, hi)
        var i = lo; var j = mid; var out = lo
        while (i < mid || j < hi) {
            if (j >= hi || (i < mid && nums[order[i]] <= nums[order[j]])) {
                Drill.compare(order[i], if (j < hi) order[j] else order[i])
                counts[order[i]] += j - mid
                Drill.write(order[i], counts[order[i]])
                buffer[out++] = order[i++]
            } else {
                buffer[out++] = order[j++]
            }
        }
        for (t in lo until hi) order[t] = buffer[t]
    }
    sort(0, n)
    return counts
}
