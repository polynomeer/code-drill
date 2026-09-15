// kind: WRONG_BRANCH
// v+1 이 없는 값을 시작점으로 보고 위로 센다. 수열의 끝에서 위로 세니 항상 1 이다.
fun longestConsecutive(nums: IntArray): Int {
    val present = HashSet<Int>()
    for (v in nums) present.add(v)
    var best = 0
    for (v in nums) {
        if (present.contains(v + 1)) continue
        var length = 1
        while (present.contains(v + length)) length += 1
        best = maxOf(best, length)
    }
    return best
}
