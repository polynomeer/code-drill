// kind: WRONG_ALGORITHM
// 둘 중 하나가 k 의 배수인 쌍만 센다. 4 × 6 = 24 처럼 둘 다 아니어도 곱은 배수일 수 있다.
fun divisibleProductPairs(nums: IntArray, k: Int): Int {
    val multiples = nums.count { it % k == 0 }.toLong()
    val others = nums.size - multiples
    val total = multiples * (multiples - 1) / 2 + multiples * others
    return (total % 1_000_000_007).toInt()
}
