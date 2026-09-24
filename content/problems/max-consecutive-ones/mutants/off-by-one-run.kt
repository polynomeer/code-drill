// kind: OFF_BY_ONE
// 이어지는 길이를 0 이 아니라 1 에서 다시 시작한다. 0 뒤의 구간이 하나씩 길어진다.
fun maxConsecutiveOnes(nums: IntArray): Int {
    var best = 0
    var run = 0
    for (value in nums) {
        if (value == 1) { run += 1; if (run > best) best = run } else run = 1
    }
    return best
}
