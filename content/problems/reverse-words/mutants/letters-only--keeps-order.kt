// kind: MISSING_EDGE_CASE
// 글자만 뒤집고 배열 순서는 그대로 둔다.
fun reverseWords(words: Array<String>): Array<String> =
    Array(words.size) { words[it].reversed() }
