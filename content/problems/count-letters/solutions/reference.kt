// 검증용 정답 (§6.1 solutions/).
//
// 알파벳인지 먼저 보고 나서 인덱스를 만든다. 순서를 바꾸면 한글이나 기호에서
// 배열 범위를 벗어난다 — 입력에 무엇이 올지는 우리가 정하지 않는다.
fun countLetters(text: String): IntArray {
    val counts = IntArray(26)

    for (index in text.indices) {
        Drill.visit(index, 0)
        val lower = text[index].lowercaseChar()
        if (lower < 'a' || lower > 'z') continue

        val slot = lower - 'a'
        counts[slot] += 1
        Drill.write(slot, counts[slot])
    }
    return counts
}
