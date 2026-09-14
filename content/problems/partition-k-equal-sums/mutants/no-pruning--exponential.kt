// kind: PERFORMANCE
// 가지치기 없이 모든 배치를 시험한다. 4^16 이다.
fun canPartition(nums: IntArray, k: Int): Int {
    val total = nums.sum()
    if (k <= 0 || total % k != 0) return 0
    val target = total / k
    val buckets = IntArray(k)
    fun place(i: Int): Boolean {
        if (i == nums.size) return buckets.all { it == target }
        for (b in 0 until k) {
            Drill.visit(i, b)
            buckets[b] += nums[i]
            if (place(i + 1)) return true
            buckets[b] -= nums[i]
        }
        return false
    }
    return if (place(0)) 1 else 0
}
