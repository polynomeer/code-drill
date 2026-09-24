// kind: WRONG_BRANCH
// 값을 보지 않고 자리마다 길이를 올린다. 0 도 이어진 것으로 센다.
fun maxConsecutiveOnes(nums: IntArray): Int {
    var best = 0
    var run = 0
    for (value in nums) {
        run += 1
        if (run > best) best = run
        if (value == 0) { }
    }
    return best
}
