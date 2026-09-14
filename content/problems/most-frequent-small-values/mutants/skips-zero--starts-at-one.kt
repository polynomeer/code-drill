// kind: OFF_BY_ONE
// 값 0 을 세지 않는다. 범위는 0 부터다.
fun mostFrequent(nums: IntArray): Int {
    val counts = IntArray(101)
    for (v in nums) if (v >= 1) counts[v] += 1
    var best = 1
    for (v in 2..100) if (counts[v] > counts[best]) best = v
    return best
}
