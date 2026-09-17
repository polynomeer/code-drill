// kind: WRONG_ALGORITHM
// 전부 XOR 한다. 두 번씩일 때의 요령이라 세 번씩에서는 틀린다.
fun singleNumberThrice(nums: IntArray): Int {
    var x = 0
    for (v in nums) x = x xor v
    return x
}
