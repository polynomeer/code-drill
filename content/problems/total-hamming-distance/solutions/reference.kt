// 검증용 정답 (§6.1 solutions/). 자리마다 1 의 개수 × 0 의 개수.
fun totalHamming(nums: IntArray): Int {
    var total = 0
    for (bit in 0 until 16) {
        var ones = 0
        for (v in nums) ones += (v shr bit) and 1
        Drill.visit(bit, ones)
        total += ones * (nums.size - ones)
        Drill.write(bit, total)
    }
    return total
}
