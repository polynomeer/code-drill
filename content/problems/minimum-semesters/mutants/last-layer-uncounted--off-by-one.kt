// kind: OFF_BY_ONE
// 다음 학기에 들을 과목이 생길 때만 학기를 센다. 마지막 학기가 빠진다.
fun minSemesters(n: Int, prereqs: IntArray): Int {
    val indegree = IntArray(n)
    val graph = Array(n) { mutableListOf<Int>() }
    var i = 0
    while (i < prereqs.size) { graph[prereqs[i]].add(prereqs[i + 1]); indegree[prereqs[i + 1]] += 1; i += 2 }
    var layer = (0 until n).filter { indegree[it] == 0 }
    var taken = 0
    var semesters = 0
    while (layer.isNotEmpty()) {
        taken += layer.size
        val nextLayer = mutableListOf<Int>()
        for (node in layer) for (after in graph[node]) { indegree[after] -= 1; if (indegree[after] == 0) nextLayer.add(after) }
        if (nextLayer.isNotEmpty()) semesters += 1
        layer = nextLayer
    }
    return if (taken == n) semesters else -1
}
