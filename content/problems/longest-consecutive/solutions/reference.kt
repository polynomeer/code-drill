// 검증용 정답 (§6.1 solutions/). 집합에 넣고 시작점에서만 위로 센다.
fun longestConsecutive(nums: IntArray): Int {
    val present = HashSet<Int>(nums.size * 2)
    for (v in nums) present.add(v)
    var best = 0
    for ((i, v) in nums.withIndex()) {
        if (present.contains(v - 1)) continue
        Drill.visit(i, v)
        var length = 1
        var next = v.toLong() + 1
        while (next <= Int.MAX_VALUE && present.contains(next.toInt())) { length += 1; next += 1 }
        if (length > best) { best = length; Drill.write(0, best) }
    }
    return best
}
