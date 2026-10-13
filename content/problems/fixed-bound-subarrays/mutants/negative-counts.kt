// kind: WRONG_BRANCH
// min(마지막 minK, 마지막 maxK) − bad 가 음수여도 더한다. 끊긴 뒤 아직 둘 중 하나가 없으면 0 이다.
fun fixedBoundSubarrays(nums: IntArray, minK: Int, maxK: Int): Int {
    var bad = -1; var lastMin = -1; var lastMax = -1; var count = 0L
    for (i in nums.indices) {
        val v = nums[i]
        if (v < minK || v > maxK) bad = i
        if (v == minK) lastMin = i
        if (v == maxK) lastMax = i
        count += minOf(lastMin, lastMax) - bad
    }
    return (((count % 1_000_000_007) + 1_000_000_007) % 1_000_000_007).toInt()
}
