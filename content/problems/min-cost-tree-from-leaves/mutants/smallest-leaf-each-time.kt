// kind: PERFORMANCE
// 가장 작은 잎을 찾아 작은 이웃과 묶기를 되풀이한다. 맞지만, 한 번 묶을 때마다 배열을 다 훑어 n² 이다.
fun minCostTreeFromLeaves(arr: IntArray): Int {
    val leaves = arr.toMutableList()
    var total = 0L
    while (leaves.size > 1) {
        var smallest = 0
        for (i in 1 until leaves.size) if (leaves[i] < leaves[smallest]) smallest = i
        val left = if (smallest > 0) leaves[smallest - 1] else Int.MAX_VALUE
        val right = if (smallest < leaves.size - 1) leaves[smallest + 1] else Int.MAX_VALUE
        total += leaves[smallest].toLong() * minOf(left, right)
        leaves.removeAt(smallest)
    }
    return total.toInt()
}
