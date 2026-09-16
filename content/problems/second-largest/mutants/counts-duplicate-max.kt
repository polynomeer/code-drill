// kind: MISSING_EDGE_CASE
// 최댓값과 같은 값을 둘째로 센다.
fun secondLargest(nums: IntArray): Int {
    var first = Long.MIN_VALUE; var second = Long.MIN_VALUE
    for (v in nums) {
        if (v > first) { second = first; first = v.toLong() }
        else if (v > second) second = v.toLong()
    }
    return if (second == Long.MIN_VALUE) -1 else second.toInt()
}
