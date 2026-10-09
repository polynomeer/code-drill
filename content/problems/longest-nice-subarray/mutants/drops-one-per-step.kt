// kind: WRONG_BRANCH
// 겹칠 때 왼쪽에서 하나만 뺀다. 새 수가 창 안의 여럿과 겹치면 겹친 채로 창을 넓힌다.
fun longestNiceSubarray(nums: IntArray): Int {
    var best = 0; var used = 0; var left = 0
    for (right in nums.indices) {
        if (used and nums[right] != 0) { used = used xor nums[left]; left += 1 }
        used = used or nums[right]
        best = maxOf(best, right - left + 1)
    }
    return best
}
