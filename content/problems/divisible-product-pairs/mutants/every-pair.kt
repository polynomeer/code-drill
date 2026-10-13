// kind: PERFORMANCE
// 쌍을 모두 보며 곱의 나머지를 구한다. 맞지만, 10 만이면 50 억 쌍이다.
fun divisibleProductPairs(nums: IntArray, k: Int): Int {
    var total = 0L
    for (i in nums.indices) for (j in i + 1 until nums.size) if (nums[i].toLong() * nums[j] % k == 0L) total += 1
    return (total % 1_000_000_007).toInt()
}
