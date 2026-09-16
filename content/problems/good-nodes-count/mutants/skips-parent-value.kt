// kind: WRONG_BRANCH
// 내려보내는 최댓값에 부모 자신의 값을 넣지 않는다. 조부모 위로만 비교된다.
fun goodNodes(parent: IntArray, values: IntArray): Int {
    val n = parent.size
    val best = IntArray(n) { Int.MIN_VALUE }
    var count = 0
    for (i in 0 until n) {
        if (parent[i] == -1) { count += 1 } else {
            val above = best[parent[i]]
            best[i] = maxOf(above, values[parent[i]])
            if (values[i] >= above) count += 1
        }
    }
    return count
}
