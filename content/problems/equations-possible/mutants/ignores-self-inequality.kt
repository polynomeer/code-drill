// kind: MISSING_EDGE_CASE
// 같은 글자끼리의 부등식을 건너뛴다. a!=a 는 항상 모순이다.
fun equationsPossible(equations: Array<String>): Int {
    val parent = IntArray(26) { it }
    fun find(x: Int): Int { var v = x; while (parent[v] != v) v = parent[v]; return v }
    for (eq in equations) if (eq[1] == '=') parent[find(eq[0] - 'a')] = find(eq[3] - 'a')
    for (eq in equations) if (eq[1] == '!' && eq[0] != eq[3] && find(eq[0] - 'a') == find(eq[3] - 'a')) return 0
    return 1
}
