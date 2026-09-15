// 검증용 정답 (§6.1 solutions/). 글자를 정렬한 문자열이 묶음의 이름표다.
fun anagramGroups(words: Array<String>): Int {
    val labels = HashSet<String>()
    for ((i, word) in words.withIndex()) {
        Drill.visit(i, 0)
        if (labels.add(String(word.toCharArray().sortedArray()))) Drill.write(0, labels.size)
    }
    return labels.size
}
