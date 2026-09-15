// kind: MISSING_EDGE_CASE
// 정렬해 이웃이 같거나 1 차이면 잇는다. 같은 값이 길이에 들어간다.
fun longestConsecutive(nums: IntArray): Int {
    if (nums.isEmpty()) return 0
    val sorted = nums.sorted()
    var best = 1; var length = 1
    for (i in 1 until sorted.size) {
        if (sorted[i] - sorted[i - 1] <= 1) length += 1 else length = 1
        best = maxOf(best, length)
    }
    return best
}
