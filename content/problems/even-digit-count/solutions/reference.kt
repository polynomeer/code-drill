// 검증용 정답 (§6.1 solutions/). 수마다 10 으로 나눠 가며 센다 — 적어도 한 자리.
fun evenDigitCount(nums: IntArray): Int {
    var count = 0
    for (i in nums.indices) {
        var x = nums[i]
        var digits = 1
        while (x / 10 != 0) { x /= 10; digits += 1 }
        Drill.write(i, digits)
        if (digits % 2 == 0) count += 1
    }
    return count
}
