// kind: WRONG_ALGORITHM
// 배열 전체의 (최댓값 − 최솟값)에 구간 수를 곱한다. 구간마다 폭이 다르다.
fun subarrayRangesSum(nums: IntArray): Int {
    val mod = 1_000_000_007L
    val range = (nums.maxOrNull()!!.toLong() - nums.minOrNull()!!) % mod
    val count = nums.size.toLong() * (nums.size + 1) / 2 % mod
    return (range * count % mod).toInt()
}
