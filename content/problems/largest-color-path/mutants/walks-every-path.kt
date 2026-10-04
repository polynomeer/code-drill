// kind: PERFORMANCE
// 시작점마다 모든 경로를 따라가며 색을 센다. 갈래가 겹치면 경로 수가 곱으로 불어난다.
fun largestColorPath(colors: String, edges: IntArray): Int {
    val n = colors.length
    val adj = Array(n) { ArrayList<Int>() }
    val indegree = IntArray(n)
    for (i in edges.indices step 2) { adj[edges[i]].add(edges[i + 1]); indegree[edges[i + 1]] += 1 }
    val left = indegree.copyOf()
    val queue = java.util.ArrayDeque<Int>()
    for (v in 0 until n) if (left[v] == 0) queue.add(v)
    var seen = 0
    while (queue.isNotEmpty()) { val v = queue.poll(); seen += 1; for (u in adj[v]) { left[u] -= 1; if (left[u] == 0) queue.add(u) } }
    if (seen < n) return -1
    val count = IntArray(26)
    var answer = 0
    val stack = java.util.ArrayDeque<IntArray>()
    for (s in 0 until n) {
        if (indegree[s] != 0) continue
        stack.push(intArrayOf(s, 0))
        count[colors[s] - 'a'] += 1
        answer = maxOf(answer, count[colors[s] - 'a'])
        while (stack.isNotEmpty()) {
            val top = stack.peek()
            val v = top[0]
            if (top[1] < adj[v].size) {
                val u = adj[v][top[1]]
                top[1] += 1
                Drill.compare(v, u)
                count[colors[u] - 'a'] += 1
                answer = maxOf(answer, count[colors[u] - 'a'])
                stack.push(intArrayOf(u, 0))
            } else {
                count[colors[v] - 'a'] -= 1
                stack.pop()
            }
        }
    }
    return answer
}
