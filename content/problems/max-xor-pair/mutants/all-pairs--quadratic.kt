// kind: PERFORMANCE
// 모든 쌍을 본다. O(n²).
fun maxXorPair(nums: IntArray): Int {
    var best = 0
    for (i in nums.indices) for (j in i + 1 until nums.size) {
        Drill.compare(i, j)
        val v = nums[i] xor nums[j]
        if (v > best) best = v
    }
    return best
}
