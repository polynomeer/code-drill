// kind: OFF_BY_ONE
// 어긋난 글자까지 답에 넣는다.
fun commonPrefix(words: Array<String>): String {
    var shortest = words[0]
    for (word in words) if (word.length < shortest.length) shortest = word
    for (index in shortest.indices) {
        for (word in words) {
            if (word[index] != shortest[index]) return shortest.substring(0, index + 1)
        }
    }
    return shortest
}
