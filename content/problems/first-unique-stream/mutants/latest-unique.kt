// kind: WRONG_BRANCH
// 한 번만 나온 글자 가운데 가장 늦게 들어온 것을 적는다. 가장 먼저 들어온 것이다.
fun firstUniqueStream(s: String): String {
    val counts = IntArray(26); val out = StringBuilder()
    for (i in s.indices) {
        counts[s[i] - 'a'] += 1
        var pick = '#'
        for (j in i downTo maxOf(0, i - 60)) if (counts[s[j] - 'a'] == 1) { pick = s[j]; break }
        out.append(pick)
    }
    return out.toString()
}
