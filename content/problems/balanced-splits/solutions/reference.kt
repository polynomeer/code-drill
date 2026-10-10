// 검증용 정답 (§6.1 solutions/). 전체 합을 구하고, 왼쪽 누적합의 두 배가 전체와 같은 자르기를 센다. Long 으로.
fun balancedSplits(nums: IntArray): Int {
    var total = 0L
    for (v in nums) total += v
    var left = 0L
    var count = 0
    for (i in 0 until nums.size - 1) {
        left += nums[i]
        if (2 * left == total) { count += 1; Drill.pointer("cut", i) }
    }
    return count
}
