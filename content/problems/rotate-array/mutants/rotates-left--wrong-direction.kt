// kind: WRONG_BRANCH
// 왼쪽으로 회전한다.
fun rotate(nums: IntArray, k: Int): IntArray {
    val n = nums.size
    val shift = k % n
    val out = IntArray(n)
    for (index in nums.indices) out[(index - shift + n) % n] = nums[index]
    return out
}
