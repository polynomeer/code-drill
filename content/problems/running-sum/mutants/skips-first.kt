// kind: OFF_BY_ONE
// 1 번부터 시작해 첫 원소를 누적에 넣지 않는다.
fun runningSum(nums: IntArray): IntArray {
    val out = IntArray(nums.size)
    for (i in 1 until nums.size) out[i] = out[i - 1] + nums[i]
    return out
}
