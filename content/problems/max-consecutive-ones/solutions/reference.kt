// 검증용 정답 (§6.1 solutions/). 이어지는 길이와 최댓값 둘만 들고 한 번 훑는다.
fun maxConsecutiveOnes(nums: IntArray): Int {
    var best = 0
    var run = 0
    for (i in nums.indices) {
        Drill.visit(i, nums[i])
        if (nums[i] == 1) {
            run += 1
            if (run > best) { best = run; Drill.write(0, best) }
        } else {
            run = 0
        }
    }
    return best
}
