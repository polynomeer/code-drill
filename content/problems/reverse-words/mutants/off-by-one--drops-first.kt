// kind: OFF_BY_ONE
// 첫 단어를 빠뜨린다.
fun reverseWords(words: Array<String>): Array<String> {
    val out = Array(words.size) { "" }
    for (index in 1 until words.size) {
        out[words.size - 1 - index] = words[index].reversed()
    }
    return out
}
