// kind: OFF_BY_ONE
// 누적합 배열의 0 번을 빼고 세어 원소 하나짜리 구간을 놓친다.
fun countRangeSums(nums: IntArray, lower: Int, upper: Int): Int {
    val n = nums.size
    val prefix = LongArray(n + 1)
    for (i in 0 until n) prefix[i + 1] = prefix[i] + nums[i]
    val buffer = LongArray(n + 1)
    fun count(lo: Int, hi: Int): Int {
        if (hi - lo <= 1) return 0
        val mid = (lo + hi) / 2
        var total = count(lo, mid) + count(mid, hi)
        var j = mid; var k = mid
        for (i in lo until mid) {
            while (j < hi && prefix[j] - prefix[i] < lower) j += 1
            while (k < hi && prefix[k] - prefix[i] <= upper) k += 1
            total += k - j
        }
        var a = lo; var b = mid; var out = lo
        while (a < mid || b < hi) {
            if (b >= hi || (a < mid && prefix[a] <= prefix[b])) { buffer[out] = prefix[a]; a += 1 } else { buffer[out] = prefix[b]; b += 1 }
            out += 1
        }
        for (t in lo until hi) prefix[t] = buffer[t]
        return total
    }
    return count(1, n + 1)
}
