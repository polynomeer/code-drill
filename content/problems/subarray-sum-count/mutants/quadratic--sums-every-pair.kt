// kind: PERFORMANCE
// 모든 구간의 합을 다시 더해 O(n^2) 다. 작은 입력은 통과한다.
fun countSubarrays(nums: IntArray, k: Int): Int {
    var count = 0
    for (start in nums.indices) {
        var total = 0
        for (end in start until nums.size) {
            total += nums[end]
            if (total == k) count += 1
        }
    }
    return count
}
