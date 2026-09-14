// kind: PERFORMANCE
// 시작점마다 끝점을 늘려 가며 합을 센다. O(n²).
fun divisibleSubarrays(nums: IntArray, k: Int): Int {
    var total = 0
    for (i in nums.indices) {
        var sum = 0L
        for (j in i until nums.size) {
            Drill.compare(i, j)
            sum += nums[j]
            if (sum % k == 0L) total += 1
        }
    }
    return total
}
