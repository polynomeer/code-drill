// kind: MISSING_EDGE_CASE
// k 를 길이로 줄이지 않아 k 가 길이보다 크면 자리를 잘못 잡는다.
fun rotate(nums: IntArray, k: Int): IntArray {
    val n = nums.size
    val out = IntArray(n)
    for (index in nums.indices) out[(index + k) % n] = nums[index]
    return if (k < n) out else nums.copyOf()
}
