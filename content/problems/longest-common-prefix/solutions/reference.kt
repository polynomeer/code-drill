// 검증용 정답 (§6.1 solutions/).
//
// 가장 짧은 단어보다 긴 답은 나올 수 없다. 그것을 후보로 두고 글자마다 전부 견주면,
// 어긋나는 첫 자리가 곧 답의 끝이다.
fun commonPrefix(words: Array<String>): String {
    var shortest = words[0]
    for (word in words) if (word.length < shortest.length) shortest = word

    for (index in shortest.indices) {
        Drill.visit(index, 0)
        for (word in words) {
            Drill.compare(index, index)
            if (word[index] != shortest[index]) return shortest.substring(0, index)
        }
    }
    return shortest
}
