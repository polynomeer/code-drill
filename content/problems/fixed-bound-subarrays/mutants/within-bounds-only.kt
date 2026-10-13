// kind: WRONG_ALGORITHM
// 모든 값이 [minK, maxK] 안에 있는 구간을 센다. 최솟값과 최댓값이 정확히 minK, maxK 여야 한다.
fun fixedBoundSubarrays(nums: IntArray, minK: Int, maxK: Int): Int {
    var bad = -1; var count = 0L
    for (i in nums.indices) { if (nums[i] < minK || nums[i] > maxK) bad = i; count += i - bad }
    return (count % 1_000_000_007).toInt()
}
