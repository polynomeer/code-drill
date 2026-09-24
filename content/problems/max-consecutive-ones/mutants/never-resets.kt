// kind: WRONG_ALGORITHM
// 0 을 만나도 이어지는 길이를 되돌리지 않는다. 1 의 전체 개수를 센다.
fun maxConsecutiveOnes(nums: IntArray): Int {
    var best = 0
    var run = 0
    for (value in nums) {
        if (value == 1) { run += 1; if (run > best) best = run }
    }
    return best
}
