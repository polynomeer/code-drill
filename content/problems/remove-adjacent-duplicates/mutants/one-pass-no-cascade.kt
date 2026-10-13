// kind: WRONG_ALGORITHM
// 한 번 훑으며 붙은 쌍만 지운다. 지우고 나서 새로 붙는 쌍은 남는다.
fun removeAdjacentDuplicates(s: String): String {
    val out = StringBuilder()
    var i = 0
    while (i < s.length) {
        if (i + 1 < s.length && s[i] == s[i + 1]) i += 2 else { out.append(s[i]); i += 1 }
    }
    return out.toString()
}
