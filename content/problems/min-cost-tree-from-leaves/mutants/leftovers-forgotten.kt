// kind: MISSING_EDGE_CASE
// 다 훑은 뒤 스택에 남은 잎을 묶지 않는다. 감소하는 구간은 끝까지 스택에 남는다.
fun minCostTreeFromLeaves(arr: IntArray): Int {
    val stack = ArrayList<Int>(); stack.add(Int.MAX_VALUE)
    var total = 0L
    for (v in arr) {
        while (stack.last() <= v) { val mid = stack.removeAt(stack.size - 1); total += mid.toLong() * minOf(stack.last(), v) }
        stack.add(v)
    }
    return total.toInt()
}
