// kind: MISSING_EDGE_CASE
// 음수 누적 합의 나머지를 0 이상으로 맞추지 않는다. -2 와 3 을 다른 나머지로 센다.
fun divisibleSubarrays(nums: IntArray, k: Int): Int {
    val counts = HashMap<Int, Int>()
    counts[0] = 1
    var prefix = 0
    var total = 0
    for (v in nums) {
        prefix = (prefix + v) % k
        total += counts[prefix] ?: 0
        counts[prefix] = (counts[prefix] ?: 0) + 1
    }
    return total
}
