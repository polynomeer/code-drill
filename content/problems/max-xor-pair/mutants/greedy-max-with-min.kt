// kind: WRONG_ALGORITHM
// 최댓값과 최솟값의 XOR 을 답한다.
fun maxXorPair(nums: IntArray): Int {
    if (nums.size < 2) return 0
    return nums.max() xor nums.min()
}
