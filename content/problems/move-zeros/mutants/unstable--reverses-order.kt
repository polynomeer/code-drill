// kind: WRONG_BRANCH
// 뒤에서부터 채워 0 이 아닌 원소의 순서가 뒤집힌다.
fun moveZeros(nums: IntArray): IntArray {
    val out = IntArray(nums.size)
    var write = 0
    for (index in nums.indices.reversed()) {
        if (nums[index] != 0) {
            out[write] = nums[index]
            write += 1
        }
    }
    return out
}
