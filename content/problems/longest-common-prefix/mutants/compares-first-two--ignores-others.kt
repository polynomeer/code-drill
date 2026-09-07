// kind: MISSING_EDGE_CASE
// 앞의 두 단어만 견준다.
fun commonPrefix(words: Array<String>): String {
    if (words.size == 1) return words[0]
    val a = words[0]
    val b = words[1]
    var index = 0
    while (index < a.length && index < b.length && a[index] == b[index]) index += 1
    return a.substring(0, index)
}
