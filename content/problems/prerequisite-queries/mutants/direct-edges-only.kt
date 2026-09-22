// kind: WRONG_ALGORITHM
// 직접 선수 관계만 본다. 간접 선수를 놓친다.
fun prerequisiteQueries(n: Int, edges: IntArray, queries: IntArray): IntArray {
    val direct = HashSet<Long>()
    for (i in edges.indices step 2) direct.add(edges[i].toLong() * n + edges[i + 1])
    return IntArray(queries.size / 2) { q -> if (queries[2 * q].toLong() * n + queries[2 * q + 1] in direct) 1 else 0 }
}
