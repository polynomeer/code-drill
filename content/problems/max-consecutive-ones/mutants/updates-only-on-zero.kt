// kind: MISSING_EDGE_CASE
// 0 을 만났을 때만 최댓값을 갱신한다. 1 로 끝나는 배열의 마지막 구간이 빠진다.
fun maxConsecutiveOnes(nums: IntArray): Int {
    var best = 0
    var run = 0
    for (value in nums) {
        if (value == 1) run += 1
        else { if (run > best) best = run; run = 0 }
    }
    return best
}
