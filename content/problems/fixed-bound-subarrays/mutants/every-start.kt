// kind: PERFORMANCE
// 시작마다 끝을 늘려 가며 최솟값과 최댓값을 갱신한다. 맞지만, 범위 안의 값만 있으면 n² 이다.
fun fixedBoundSubarrays(nums: IntArray, minK: Int, maxK: Int): Int {
    var count = 0L
    for (start in nums.indices) {
        var low = Int.MAX_VALUE; var high = Int.MIN_VALUE
        for (end in start until nums.size) {
            val v = nums[end]
            if (v < minK || v > maxK) break
            if (v < low) low = v
            if (v > high) high = v
            if (low == minK && high == maxK) count += 1
        }
    }
    return (count % 1_000_000_007).toInt()
}
