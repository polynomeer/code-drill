// kind: PERFORMANCE
// 모든 정점에서 시작해 아래로 다 내려가 본다. 사슬에서 O(n²).
fun pathSumCount(parent: IntArray, values: IntArray, target: Int): Int {
    val n = parent.size
    val children = Array(n) { mutableListOf<Int>() }
    for (i in 0 until n) if (parent[i] != -1) children[parent[i]].add(i)
    var count = 0
    val stack = ArrayDeque<Pair<Int, Long>>()
    for (start in 0 until n) {
        stack.addLast(start to 0L)
        while (stack.isNotEmpty()) {
            val (v, above) = stack.removeLast()
            val sum = above + values[v]
            Drill.compare(v, start)
            if (sum == target.toLong()) count += 1
            for (c in children[v]) stack.addLast(c to sum)
        }
    }
    return count
}
