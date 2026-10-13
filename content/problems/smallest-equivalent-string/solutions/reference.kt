// 검증용 정답 (§6.1 solutions/). 유니온 파인드 — 묶을 때 더 작은 글자를 대표로 남기면 대표가 그 무리의 가장 앞선 글자다.
fun smallestEquivalentString(s1: String, s2: String, base: String): String {
    val parent = IntArray(26) { it }
    fun find(x: Int): Int {
        var r = x
        while (parent[r] != r) { parent[r] = parent[parent[r]]; r = parent[r] }
        return r
    }
    for (i in s1.indices) {
        val ra = find(s1[i] - 'a'); val rb = find(s2[i] - 'a')
        if (ra != rb) {
            if (ra < rb) parent[rb] = ra else parent[ra] = rb
            Drill.edge(('a' + ra).toString(), ('a' + rb).toString())
        }
    }
    val out = StringBuilder()
    for (c in base) out.append('a' + find(c - 'a'))
    return out.toString()
}
