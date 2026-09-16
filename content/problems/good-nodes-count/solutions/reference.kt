// 검증용 정답 (§6.1 solutions/). 부모가 앞에 오므로 번호 순서로 최댓값을 내려보낸다.
fun goodNodes(parent: IntArray, values: IntArray): Int {
    val n = parent.size
    val best = IntArray(n)
    var count = 0
    for (i in 0 until n) {
        Drill.visit(i, values[i])
        if (parent[i] == -1) {
            best[i] = values[i]
            count += 1
        } else {
            val above = best[parent[i]]
            Drill.compare(values[i], above)
            best[i] = maxOf(above, values[i])
            if (values[i] >= above) count += 1
        }
    }
    return count
}
