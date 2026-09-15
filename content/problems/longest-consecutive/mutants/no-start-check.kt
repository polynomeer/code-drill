// kind: PERFORMANCE
// 모든 값에서 위로 센다. 긴 수열 하나가 길이의 제곱이다.
fun longestConsecutive(nums: IntArray): Int {
    val present = HashSet<Int>()
    for (v in nums) present.add(v)
    var best = 0
    for (v in nums) {
        var length = 1
        var next = v.toLong() + 1
        while (next <= Int.MAX_VALUE && present.contains(next.toInt())) { Drill.compare(v, next.toInt()); length += 1; next += 1 }
        best = maxOf(best, length)
    }
    return best
}
