// kind: MISSING_EDGE_CASE
// 0 이 될 때까지 나눈 횟수로 센다. 0 은 한 자리인데 0 자리가 된다.
fun evenDigitCount(nums: IntArray): Int {
    var count = 0
    for (v in nums) {
        var x = v
        var digits = 0
        while (x != 0) { x /= 10; digits += 1 }
        if (digits % 2 == 0) count += 1
    }
    return count
}
