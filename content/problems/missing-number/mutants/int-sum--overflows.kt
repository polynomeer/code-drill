// kind: MISSING_EDGE_CASE
// 0..n 의 합을 Int 로 구한다. n 이 20 만이면 넘친다.
fun missingNumber(nums: IntArray): Int {
    val n = nums.size
    val expected = n * (n + 1) / 2
    return expected - nums.sum()
}
