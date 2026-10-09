// kind: WRONG_BRANCH
// 작은 잎을 두 이웃 가운데 큰 쪽과 묶는다. 작은 쪽과 묶어야 싸다.
fun minCostTreeFromLeaves(arr: IntArray): Int {
    val stack = ArrayList<Int>()
    var total = 0L
    for (v in arr) {
        while (stack.isNotEmpty() && stack.last() <= v) {
            val mid = stack.removeAt(stack.size - 1)
            val left = if (stack.isEmpty()) v else stack.last()
            total += mid.toLong() * maxOf(left, v)
        }
        stack.add(v)
    }
    while (stack.size > 1) { val top = stack.removeAt(stack.size - 1); total += top.toLong() * stack.last() }
    return total.toInt()
}
