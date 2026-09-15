// 검증용 정답 (§6.1 solutions/). 등식을 전부 합친 뒤 부등식을 본다.
fun equationsPossible(equations: Array<String>): Int {
    val parent = IntArray(26) { it }
    fun find(x: Int): Int { var v = x; while (parent[v] != v) { parent[v] = parent[parent[v]]; v = parent[v] }; return v }
    for (eq in equations) if (eq[1] == '=') {
        val a = find(eq[0] - 'a'); val b = find(eq[3] - 'a')
        if (a != b) { parent[a] = b; Drill.match(a, b) }
    }
    for (eq in equations) if (eq[1] == '!') {
        val a = eq[0] - 'a'; val b = eq[3] - 'a'
        Drill.compare(a, b)
        if (find(a) == find(b)) return 0
    }
    return 1
}
