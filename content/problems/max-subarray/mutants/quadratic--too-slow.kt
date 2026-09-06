// kind: PERFORMANCE
// O(n^2). 작은 입력은 통과하고 큰 입력에서만 시간 초과가 난다 — 부분 점수 그룹의 존재 이유다.
fun maxSubarray(nums: IntArray): Int {
    var best = nums[0]
    for (i in nums.indices) {
        var sum = 0
        for (j in i until nums.size) {
            sum += nums[j]
            if (sum > best) best = sum
        }
    }
    return best
}
