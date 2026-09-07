// kind: OFF_BY_ONE
// 마지막 원소를 보지 않는다.
fun moveZeros(nums: IntArray): IntArray {
    val out = IntArray(nums.size)
    var write = 0
    for (index in 0 until nums.size - 1) {
        if (nums[index] != 0) {
            out[write] = nums[index]
            write += 1
        }
    }
    return out
}
