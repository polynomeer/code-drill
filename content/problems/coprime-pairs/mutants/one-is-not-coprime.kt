// kind: MISSING_EDGE_CASE
// 1 이 든 쌍을 세지 않는다. 1 은 모든 수와 서로소다.
fun coprimePairs(nums: IntArray): Int {
    fun gcd(a: Int, b: Int): Int { var x = a; var y = b; while (y != 0) { val t = x % y; x = y; y = t }; return x }
    var count = 0
    for (i in nums.indices) for (j in i + 1 until nums.size) if (nums[i] > 1 && nums[j] > 1 && gcd(nums[i], nums[j]) == 1) count += 1
    return count
}
