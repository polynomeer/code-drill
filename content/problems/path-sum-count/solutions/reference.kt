// 검증용 정답 (§6.1 solutions/). 반복 DFS 로 조상들의 누적합을 세고 되돌아올 때 뺀다.
fun pathSumCount(parent: IntArray, values: IntArray, target: Int): Int {
    val n = parent.size
    val head = IntArray(n) { -1 }
    val next = IntArray(n)
    var root = 0
    for (i in 0 until n) {
        if (parent[i] == -1) { root = i; continue }
        next[i] = head[parent[i]]; head[parent[i]] = i
    }
    val prefix = LongArray(n)
    val seen = HashMap<Long, Int>()
    seen[0L] = 1
    var count = 0
    // 스택에는 정점을 두 번 넣는다 — 들어갈 때(양수)와 나올 때(음수 표시).
    val stack = IntArray(2 * n + 2)
    var top = 0
    stack[top++] = root
    while (top > 0) {
        val item = stack[--top]
        if (item >= 0) {
            val v = item
            val above = if (parent[v] == -1) 0L else prefix[parent[v]]
            prefix[v] = above + values[v]
            Drill.visit(v, prefix[v].toInt())
            val hits = seen[prefix[v] - target] ?: 0
            count += hits
            Drill.write(v, count)
            seen[prefix[v]] = (seen[prefix[v]] ?: 0) + 1
            stack[top++] = -v - 1
            var child = head[v]
            while (child != -1) { stack[top++] = child; child = next[child] }
        } else {
            val v = -item - 1
            seen[prefix[v]] = seen[prefix[v]]!! - 1
        }
    }
    return count
}
