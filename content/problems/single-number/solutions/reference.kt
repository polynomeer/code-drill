// 검증용 정답 (§6.1 solutions/). 누적 XOR.
//
// 같은 값을 두 번 XOR 하면 0 이 되고, 0 과 XOR 하면 그대로 남는다. 그래서 전부
// XOR 하면 짝이 있는 값은 서로 지워지고 홀로 남은 값만 남는다 — 순서도, 값의
// 부호도 상관없다.
fun singleNumber(nums: IntArray): Int {
    var accumulator = 0
    for (index in nums.indices) {
        Drill.visit(index, nums[index])
        accumulator = accumulator xor nums[index]
        Drill.write(0, accumulator)
    }
    return accumulator
}
