// 검증용 정답 (§6.1 solutions/).
fun runningSum(nums: IntArray): IntArray {
    val out = IntArray(nums.size)
    var total = 0
    for ((i, v) in nums.withIndex()) {
        Drill.visit(i, v)
        total += v
        out[i] = total
        Drill.write(i, total)
    }
    return out
}
