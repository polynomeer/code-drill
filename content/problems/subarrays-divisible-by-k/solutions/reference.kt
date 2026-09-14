// 검증용 정답 (§6.1 solutions/). 나머지별 누적 합 개수.
fun divisibleSubarrays(nums: IntArray, k: Int): Int {
    val counts = IntArray(k)
    counts[0] = 1
    var prefix = 0
    var total = 0
    for ((i, v) in nums.withIndex()) {
        prefix = ((prefix + v) % k + k) % k
        Drill.visit(i, prefix)
        total += counts[prefix]
        counts[prefix] += 1
        Drill.write(prefix, counts[prefix])
    }
    return total
}
