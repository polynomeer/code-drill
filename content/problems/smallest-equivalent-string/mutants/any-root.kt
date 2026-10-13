// kind: WRONG_ALGORITHM
// 묶을 때 대표를 아무 쪽으로나 둔다. 대표가 그 무리의 가장 앞선 글자라는 보장이 없다.
fun smallestEquivalentString(s1: String, s2: String, base: String): String {
    val parent = IntArray(26) { it }
    fun find(x: Int): Int { var r = x; while (parent[r] != r) r = parent[r]; return r }
    for (i in s1.indices) { val ra = find(s1[i] - 'a'); val rb = find(s2[i] - 'a'); if (ra != rb) parent[ra] = rb }
    return base.map { 'a' + find(it - 'a') }.joinToString("")
}
