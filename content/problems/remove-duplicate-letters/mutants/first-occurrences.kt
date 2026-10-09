// kind: WRONG_ALGORITHM
// 글자마다 처음 나온 것을 남긴다. 사전순으로 가장 앞서는 선택이 아니다.
fun removeDuplicateLetters(s: String): String {
    val seen = BooleanArray(26)
    val out = StringBuilder()
    for (c in s) if (!seen[c - 'a']) { seen[c - 'a'] = true; out.append(c) }
    return out.toString()
}
