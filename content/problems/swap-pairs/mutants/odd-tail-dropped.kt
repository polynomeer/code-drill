// kind: MISSING_EDGE_CASE
// 짝이 없는 마지막 원소를 버린다. 그대로 두어야 한다.
fun swapPairs(nums: IntArray): IntArray {
    val out = IntArray(nums.size / 2 * 2)
    for (i in out.indices step 2) { out[i] = nums[i + 1]; out[i + 1] = nums[i] }
    return out
}
