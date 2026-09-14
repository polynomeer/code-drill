// 검증용 정답 (§6.1 solutions/). Boyer–Moore 투표.
fun majority(nums: IntArray): Int {
    var candidate = nums[0]
    var count = 0
    for ((i, v) in nums.withIndex()) {
        if (count == 0) { candidate = v; Drill.write(0, candidate) }
        count += if (v == candidate) 1 else -1
        Drill.visit(i, count)
    }
    return candidate
}
