// kind: WRONG_BRANCH
// 동률이면 나중에 본 값으로 바꾼다. 가장 작은 값이어야 한다.
fun mostFrequent(nums: IntArray): Int {
    val counts = IntArray(101)
    for (v in nums) counts[v] += 1
    var best = 0
    for (v in 1..100) if (counts[v] >= counts[best]) best = v
    return best
}
