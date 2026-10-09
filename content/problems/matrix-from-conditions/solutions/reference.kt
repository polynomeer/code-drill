// 검증용 정답 (§6.1 solutions/). 행과 열 각각 최소 힙 Kahn 으로 위상 순서를 얻고, 두 자리가 만나는 칸에 수를 놓는다.
fun buildMatrix(k: Int, rowConditions: IntArray, colConditions: IntArray): Array<IntArray> {
    fun order(conditions: IntArray): IntArray? {
        val adj = Array(k + 1) { ArrayList<Int>() }
        val indegree = IntArray(k + 1)
        for (i in conditions.indices step 2) { adj[conditions[i]].add(conditions[i + 1]); indegree[conditions[i + 1]] += 1 }
        val ready = java.util.PriorityQueue<Int>()
        for (v in 1..k) if (indegree[v] == 0) ready.add(v)
        val out = IntArray(k)
        var size = 0
        while (ready.isNotEmpty()) {
            val v = ready.poll()
            out[size++] = v
            Drill.dequeue(v)
            for (u in adj[v]) { indegree[u] -= 1; if (indegree[u] == 0) ready.add(u) }
        }
        return if (size == k) out else null
    }
    val rows = order(rowConditions) ?: return arrayOf()
    val cols = order(colConditions) ?: return arrayOf()
    val rowOf = IntArray(k + 1); val colOf = IntArray(k + 1)
    for (i in 0 until k) { rowOf[rows[i]] = i; colOf[cols[i]] = i }
    val matrix = Array(k) { IntArray(k) }
    for (v in 1..k) matrix[rowOf[v]][colOf[v]] = v
    return matrix
}
