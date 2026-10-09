// kind: WRONG_ALGORITHM
// 나온 글자를 정렬해 이어 붙인다. 원래 순서를 지켜야 한다.
fun removeDuplicateLetters(s: String): String = s.toSortedSet().joinToString("")
