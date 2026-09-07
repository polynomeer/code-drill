// kind: OFF_BY_ONE
// 한 칸 더 민다.
fun rotate(nums: IntArray, k: Int): IntArray {
    val n = nums.size
    val shift = (k + 1) % n
    val out = IntArray(n)
    for (index in nums.indices) out[(index + shift) % n] = nums[index]
    return out
}
