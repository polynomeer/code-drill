// kind: WRONG_ALGORITHM
// 원소가 양수인 것처럼 슬라이딩 윈도우를 돌린다. 음수가 있으면 창을 줄여도 합이 늘 수 있다.
fun shortestSubarrayAtLeastK(nums: IntArray, k: Int): Int {
    var best = nums.size + 1
    var sum = 0L
    var left = 0
    for (right in nums.indices) {
        sum += nums[right]
        while (left <= right && sum >= k) { best = minOf(best, right - left + 1); sum -= nums[left]; left += 1 }
    }
    return if (best <= nums.size) best else -1
}
