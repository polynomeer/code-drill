// kind: WRONG_BRANCH
// xor 대신 or 를 쓴다. 짝이 서로를 지우지 않아 비트가 쌓이기만 한다.
fun singleNumber(nums: IntArray): Int {
    var accumulator = 0
    for (value in nums) accumulator = accumulator or value
    return accumulator
}
