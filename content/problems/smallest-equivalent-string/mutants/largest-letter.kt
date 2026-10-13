// kind: WRONG_BRANCH
// 무리에서 가장 큰 글자로 바꾼다. 가장 앞선 글자다.
fun smallestEquivalentString(s1: String, s2: String, base: String): String {
    val parent = IntArray(26) { it }
    fun find(x: Int): Int { var r = x; while (parent[r] != r) r = parent[r]; return r }
    for (i in s1.indices) { val ra = find(s1[i] - 'a'); val rb = find(s2[i] - 'a'); if (ra != rb) { if (ra > rb) parent[rb] = ra else parent[ra] = rb } }
    return base.map { 'a' + find(it - 'a') }.joinToString("")
}
