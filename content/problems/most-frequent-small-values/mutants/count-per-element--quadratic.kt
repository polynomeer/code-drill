// kind: PERFORMANCE
// 원소마다 배열 전체를 훑어 자기 개수를 센다. O(n²).
fun mostFrequent(nums: IntArray): Int {
    var best = -1
    var bestCount = 0
    for (i in nums.indices) {
        var count = 0
        for (j in nums.indices) { Drill.compare(i, j); if (nums[j] == nums[i]) count += 1 }
        if (count > bestCount || (count == bestCount && nums[i] < best)) { best = nums[i]; bestCount = count }
    }
    return best
}
