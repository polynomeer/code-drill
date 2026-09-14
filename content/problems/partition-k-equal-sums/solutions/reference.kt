// 검증용 정답 (§6.1 solutions/). 큰 것부터, 같은 값의 묶음은 하나만, 넘치면 물러선다.
fun canPartition(nums: IntArray, k: Int): Int {
    val total = nums.sum()
    if (k <= 0 || total % k != 0) return 0
    val target = total / k
    val values = nums.sortedDescending()
    if (values.first() > target) return 0
    val buckets = IntArray(k)
    fun place(i: Int): Boolean {
        if (i == values.size) return true
        val v = values[i]
        var tried = -1
        for (b in 0 until k) {
            if (buckets[b] + v > target || buckets[b] == tried) continue
            tried = buckets[b]
            Drill.visit(i, b)
            buckets[b] += v
            if (place(i + 1)) { Drill.match(i, b); return true }
            buckets[b] -= v
            if (buckets[b] == 0) break
        }
        return false
    }
    return if (place(0)) 1 else 0
}
