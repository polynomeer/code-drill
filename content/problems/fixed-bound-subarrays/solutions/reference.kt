// 검증용 정답 (§6.1 solutions/). 오른쪽 끝마다 max(0, min(마지막 minK, 마지막 maxK) − 범위를 벗어난 마지막 자리) 를 더한다.
fun fixedBoundSubarrays(nums: IntArray, minK: Int, maxK: Int): Int {
    var bad = -1
    var lastMin = -1
    var lastMax = -1
    var count = 0L
    for (i in nums.indices) {
        val v = nums[i]
        if (v < minK || v > maxK) { bad = i; Drill.pointer("bad", bad) }
        if (v == minK) lastMin = i
        if (v == maxK) lastMax = i
        count += maxOf(0, minOf(lastMin, lastMax) - bad)
    }
    return (count % 1_000_000_007).toInt()
}
