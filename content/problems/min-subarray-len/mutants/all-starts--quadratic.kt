// kind: PERFORMANCE
// 시작점마다 합이 목표에 닿을 때까지 더한다. 닿지 않으면 끝까지 — O(n²).
fun minSubarrayLen(nums: IntArray, target: Int): Int {
    var best = 0
    for (i in nums.indices) {
        var total = 0L
        for (j in i until nums.size) {
            total += nums[j]
            Drill.compare(i, j)
            if (total >= target) { if (best == 0 || j - i + 1 < best) best = j - i + 1; break }
        }
    }
    return best
}
