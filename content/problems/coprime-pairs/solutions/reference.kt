// 검증용 정답 (§6.1 solutions/). 쌍마다 유클리드 호제법으로 최대공약수를 구한다.
fun coprimePairs(nums: IntArray): Int {
    fun gcd(a: Int, b: Int): Int { var x = a; var y = b; while (y != 0) { val t = x % y; x = y; y = t }; return x }
    var count = 0
    for (i in nums.indices) for (j in i + 1 until nums.size) {
        if (gcd(nums[i], nums[j]) == 1) count += 1
    }
    Drill.compare(0, nums.size - 1)
    return count
}
