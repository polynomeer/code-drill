// kind: MISSING_EDGE_CASE
// 문자열 길이로 센다. 음수의 부호까지 한 자리로 센다.
fun evenDigitCount(nums: IntArray): Int = nums.count { it.toString().length % 2 == 0 }
