// kind: WRONG_ALGORITHM
// 식을 순서대로 처리한다. 뒤에 오는 등식이 앞의 부등식을 깨뜨리는 것을 놓친다.
fun equationsPossible(equations: Array<String>): Int {
    val parent = IntArray(26) { it }
    fun find(x: Int): Int { var v = x; while (parent[v] != v) v = parent[v]; return v }
    for (eq in equations) {
        val a = find(eq[0] - 'a'); val b = find(eq[3] - 'a')
        if (eq[1] == '=') parent[a] = b else if (a == b) return 0
    }
    return 1
}
