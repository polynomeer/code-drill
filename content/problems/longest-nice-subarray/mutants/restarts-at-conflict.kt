// kind: WRONG_ALGORITHM
// 겹치면 창을 새 수 하나로 새로 시작한다. 겹친 수 뒤의 수들은 새 수와 함께 남을 수 있다.
fun longestNiceSubarray(nums: IntArray): Int {
    var best = 0; var used = 0; var length = 0
    for (v in nums) {
        if (used and v != 0) { used = 0; length = 0 }
        used = used or v; length += 1
        best = maxOf(best, length)
    }
    return best
}
