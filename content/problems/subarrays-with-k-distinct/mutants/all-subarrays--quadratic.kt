// kind: PERFORMANCE
// 모든 구간의 종류를 센다. O(n²).
fun subarraysWithKDistinct(nums: IntArray, k: Int): Int {
    var total = 0
    val counts = IntArray(nums.size + 1)
    for (i in nums.indices) {
        counts.fill(0)
        var kinds = 0
        for (j in i until nums.size) {
            Drill.compare(i, j)
            if (counts[nums[j]]++ == 0) kinds += 1
            if (kinds > k) break
            if (kinds == k) total += 1
        }
    }
    return total
}
