// kind: WRONG_BRANCH
// 왼쪽 수를 뺄 때 OR 을 다시 한다. OR 은 비트를 지우지 못해 창이 다시 겹치지 않는 때를 알아채지 못한다.
fun longestNiceSubarray(nums: IntArray): Int {
    var best = 0; var used = 0; var left = 0
    for (right in nums.indices) {
        while (left < right && used and nums[right] != 0) { used = used or nums[left]; left += 1 }
        if (left == right) used = 0
        used = used or nums[right]
        best = maxOf(best, right - left + 1)
    }
    return best
}
