// 검증용 정답 (§6.1 solutions/).
//
// 배열 순서와 글자 순서는 서로 다른 뒤집기다. 하나만 하면 원소가 하나이거나 단어가
// 전부 회문일 때만 우연히 맞는다.
fun reverseWords(words: Array<String>): Array<String> {
    val out = Array(words.size) { "" }
    for (index in words.indices) {
        Drill.visit(index, 0)
        val target = words.size - 1 - index
        Drill.swap(index, target)
        out[target] = words[index].reversed()
    }
    return out
}
