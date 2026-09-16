// kind: PERFORMANCE
// 원소마다 처음부터 다시 더한다. O(n²).
fun runningSum(nums: IntArray): IntArray {
    val out = IntArray(nums.size)
    for (i in nums.indices) {
        var total = 0
        for (j in 0..i) { Drill.visit(j, nums[j]); total += nums[j] }
        out[i] = total
    }
    return out
}
