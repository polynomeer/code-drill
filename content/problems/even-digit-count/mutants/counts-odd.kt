// kind: WRONG_BRANCH
// 자릿수가 홀수인 수를 센다. 짝수인 수다.
fun evenDigitCount(nums: IntArray): Int {
    var count = 0
    for (v in nums) { var x = v; var d = 1; while (x / 10 != 0) { x /= 10; d += 1 }; if (d % 2 == 1) count += 1 }
    return count
}
