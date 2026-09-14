// kind: WRONG_ALGORITHM
// 서로 다른 글자의 수를 답한다. 연속이라는 조건을 잊었다.
fun longestUnique(text: String): Int = text.toSet().size
