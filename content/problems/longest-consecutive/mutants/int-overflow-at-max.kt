// kind: MISSING_EDGE_CASE
// v + 1 을 Int 로 계산한다. Int.MAX_VALUE 다음이 Int.MIN_VALUE 가 되어 이어진다.
fun longestConsecutive(nums: IntArray): Int {
    val present = HashSet<Int>()
    for (v in nums) present.add(v)
    var best = 0
    for (v in nums) {
        if (present.contains(v - 1)) continue
        var length = 1
        while (present.contains(v + length)) length += 1
        best = maxOf(best, length)
    }
    return best
}
