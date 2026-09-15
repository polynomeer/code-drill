// kind: OFF_BY_ONE
// 0..n-1 만 XOR 한다. n 이 빠진 경우를 놓친다.
fun missingNumber(nums: IntArray): Int {
    var acc = 0
    for ((i, v) in nums.withIndex()) acc = acc xor i xor v
    return acc
}
