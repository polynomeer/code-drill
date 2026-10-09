// 검증용 정답 (§6.1 solutions/). 창의 OR 을 들고, 새 수와 겹치면 겹치지 않을 때까지 왼쪽을 XOR 로 뺀다.
fun longestNiceSubarray(nums: IntArray): Int {
    var best = 0
    var used = 0
    var left = 0
    for (right in nums.indices) {
        while (used and nums[right] != 0) { used = used xor nums[left]; left += 1; Drill.pointer("left", left) }
        used = used or nums[right]
        Drill.write(0, used)
        best = maxOf(best, right - left + 1)
    }
    return best
}
