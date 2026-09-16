// 검증용 정답 (§6.1 solutions/). 최댓값과 둘째를 한 번에 들고 간다.
fun secondLargest(nums: IntArray): Int {
    var first: Long = Long.MIN_VALUE
    var second: Long = Long.MIN_VALUE
    for ((i, v) in nums.withIndex()) {
        Drill.visit(i, v)
        if (v > first) { second = first; first = v.toLong(); Drill.write(0, v) }
        else if (v < first && v > second) { second = v.toLong(); Drill.write(1, v) }
    }
    return if (second == Long.MIN_VALUE) -1 else second.toInt()
}
