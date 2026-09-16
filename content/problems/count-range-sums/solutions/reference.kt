// 검증용 정답 (§6.1 solutions/). 누적합 위에서 병합 정렬하며 범위에 드는 쌍을 두 포인터로 센다.
fun countRangeSums(nums: IntArray, lower: Int, upper: Int): Int {
    val n = nums.size
    val prefix = LongArray(n + 1)
    for (i in 0 until n) prefix[i + 1] = prefix[i] + nums[i]
    val buffer = LongArray(n + 1)
    fun count(lo: Int, hi: Int): Int {
        if (hi - lo <= 1) return 0
        val mid = (lo + hi) / 2
        var total = count(lo, mid) + count(mid, hi)
        var j = mid
        var k = mid
        for (i in lo until mid) {
            while (j < hi && prefix[j] - prefix[i] < lower) j += 1
            while (k < hi && prefix[k] - prefix[i] <= upper) k += 1
            Drill.compare(i, k)
            total += k - j
        }
        var a = lo; var b = mid; var out = lo
        while (a < mid || b < hi) {
            if (b >= hi || (a < mid && prefix[a] <= prefix[b])) { buffer[out] = prefix[a]; a += 1 } else { buffer[out] = prefix[b]; b += 1 }
            out += 1
        }
        for (t in lo until hi) { prefix[t] = buffer[t]; Drill.write(t, prefix[t].toInt()) }
        return total
    }
    return count(0, n + 1)
}
