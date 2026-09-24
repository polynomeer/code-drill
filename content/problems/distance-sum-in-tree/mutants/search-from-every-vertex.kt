// kind: PERFORMANCE
// 정점마다 트리를 한 번씩 훑는다. O(n^2).
fun distanceSums(parent: IntArray): IntArray {
    val n = parent.size
    val head = IntArray(n) { -1 }
    val next = IntArray(n)
    for (i in n - 1 downTo 1) { next[i] = head[parent[i]]; head[parent[i]] = i }
    val out = IntArray(n)
    val stack = IntArray(n)
    val depth = IntArray(n)
    for (start in 0 until n) {
        var total = 0L
        var top = 0
        stack[top] = start; depth[start] = 0; top += 1
        val seen = IntArray(n) { -1 }
        seen[start] = 0
        while (top > 0) {
            val v = stack[--top]
            Drill.compare(start, v)
            total += depth[v]
            var c = head[v]
            while (c != -1) { if (seen[c] == -1) { seen[c] = 0; depth[c] = depth[v] + 1; stack[top++] = c }; c = next[c] }
            val p = parent[v]
            if (p != -1 && seen[p] == -1) { seen[p] = 0; depth[p] = depth[v] + 1; stack[top++] = p }
        }
        out[start] = total.toInt()
    }
    return out
}
