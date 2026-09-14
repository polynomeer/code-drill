// kind: OFF_BY_ONE
// 자리를 15 개만 본다. 가장 높은 자리가 다른 쌍을 놓친다.
fun totalHamming(nums: IntArray): Int {
    var total = 0
    for (bit in 0 until 15) {
        var ones = 0
        for (v in nums) ones += (v shr bit) and 1
        total += ones * (nums.size - ones)
    }
    return total
}
