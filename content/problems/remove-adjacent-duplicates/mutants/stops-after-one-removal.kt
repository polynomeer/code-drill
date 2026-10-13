// kind: WRONG_BRANCH
// 쌍 하나를 지우면 멈춘다. 더 지울 것이 없을 때까지 되풀이한다.
fun removeAdjacentDuplicates(s: String): String {
    for (i in 0 until s.length - 1) if (s[i] == s[i + 1]) return s.substring(0, i) + s.substring(i + 2)
    return s
}
