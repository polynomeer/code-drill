// kind: MISSING_EDGE_CASE
// 합이 목표를 넘으면 더 내려가지 않는다. 값이 음수면 나중에 다시 목표가 될 수 있다.
fun pathSumCount(parent: IntArray, values: IntArray, target: Int): Int {
    val n = parent.size
    val children = Array(n) { mutableListOf<Int>() }
    var root = 0
    for (i in 0 until n) if (parent[i] == -1) root = i else children[parent[i]].add(i)
    var count = 0
    val stack = ArrayDeque<Pair<Int, Long>>()
    for (start in 0 until n) {
        stack.addLast(start to 0L)
        while (stack.isNotEmpty()) {
            val (v, above) = stack.removeLast()
            val sum = above + values[v]
            if (sum == target.toLong()) count += 1
            if (sum > target) continue
            for (c in children[v]) stack.addLast(c to sum)
        }
    }
    return count
}
