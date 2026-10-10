// kind: WRONG_BRANCH
// 이웃한 원소끼리만 본다. 쌍은 어느 두 자리든 된다.
fun coprimePairs(nums: IntArray): Int {
    fun gcd(a: Int, b: Int): Int { var x = a; var y = b; while (y != 0) { val t = x % y; x = y; y = t }; return x }
    var count = 0
    for (i in 0 until nums.size - 1) if (gcd(nums[i], nums[i + 1]) == 1) count += 1
    return count
}
