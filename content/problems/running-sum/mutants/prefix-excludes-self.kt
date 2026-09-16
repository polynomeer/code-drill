// kind: OFF_BY_ONE
// 자기 앞까지의 합을 적는다. 한 칸 밀린다.
fun runningSum(nums: IntArray): IntArray {
    val out = IntArray(nums.size)
    var total = 0
    for (i in nums.indices) { out[i] = total; total += nums[i] }
    return out
}
