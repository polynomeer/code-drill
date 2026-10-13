// kind: MISSING_EDGE_CASE
// 구간 수를 Int 로 센다. 범위 안의 값만 10 만 개면 50 억 개라 넘친다.
fun fixedBoundSubarrays(nums: IntArray, minK: Int, maxK: Int): Int {
    var bad = -1; var lastMin = -1; var lastMax = -1; var count = 0
    for (i in nums.indices) {
        val v = nums[i]
        if (v < minK || v > maxK) bad = i
        if (v == minK) lastMin = i
        if (v == maxK) lastMax = i
        count += maxOf(0, minOf(lastMin, lastMax) - bad)
    }
    return count % 1_000_000_007
}
