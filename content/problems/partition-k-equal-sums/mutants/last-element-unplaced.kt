// kind: OFF_BY_ONE
// 마지막 원소를 놓기 전에 성공으로 본다. 마지막 하나가 들어갈 자리가 없어도 1 을 낸다.
fun canPartition(nums: IntArray, k: Int): Int {
    val total = nums.sum()
    if (k <= 0 || total % k != 0) return 0
    val target = total / k
    val values = nums.sortedDescending()
    if (values.first() > target) return 0
    val buckets = IntArray(k)
    fun place(i: Int): Boolean {
        if (i >= values.size - 1) return true
        var tried = -1
        for (b in 0 until k) {
            if (buckets[b] + values[i] > target || buckets[b] == tried) continue
            tried = buckets[b]
            buckets[b] += values[i]
            if (place(i + 1)) return true
            buckets[b] -= values[i]
            if (buckets[b] == 0) break
        }
        return false
    }
    return if (place(0)) 1 else 0
}
