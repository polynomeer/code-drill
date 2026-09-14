// kind: WRONG_BRANCH
// 1 의 개수끼리 곱한다. 서로 다른 쌍이 아니라 같은 쌍을 센다.
fun totalHamming(nums: IntArray): Int {
    var total = 0
    for (bit in 0 until 16) {
        var ones = 0
        for (v in nums) ones += (v shr bit) and 1
        total += ones * ones
    }
    return total
}
