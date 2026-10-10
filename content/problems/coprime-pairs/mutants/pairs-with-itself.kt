// kind: OFF_BY_ONE
// i <= j 로 원소를 자기 자신과도 짝짓는다. 1 은 자기 자신과 서로소라 1 하나마다 하나 더 세어진다.
fun coprimePairs(nums: IntArray): Int {
    fun gcd(a: Int, b: Int): Int { var x = a; var y = b; while (y != 0) { val t = x % y; x = y; y = t }; return x }
    var count = 0
    for (i in nums.indices) for (j in i until nums.size) if (gcd(nums[i], nums[j]) == 1) count += 1
    return count
}
