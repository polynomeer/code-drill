// kind: WRONG_BRANCH
// 직접 짝지어진 글자 가운데 가장 작은 것으로만 바꾼다. 같다는 관계는 이어진다 — a = c, c = b 면 b 는 a 다.
fun smallestEquivalentString(s1: String, s2: String, base: String): String {
    val best = CharArray(26) { 'a' + it }
    for (i in s1.indices) {
        val x = s1[i] - 'a'; val y = s2[i] - 'a'
        best[x] = minOf(best[x], s2[i]); best[y] = minOf(best[y], s1[i])
    }
    return base.map { best[it - 'a'] }.joinToString("")
}
