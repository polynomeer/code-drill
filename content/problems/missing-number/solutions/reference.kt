// 검증용 정답 (§6.1 solutions/). 0..n 과 배열을 전부 XOR 하면 짝 없는 것만 남는다.
fun missingNumber(nums: IntArray): Int {
    var acc = nums.size
    for ((i, v) in nums.withIndex()) {
        Drill.visit(i, v)
        acc = acc xor i xor v
        Drill.write(0, acc)
    }
    return acc
}
