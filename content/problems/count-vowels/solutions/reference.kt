// 검증용 정답 (§6.1 solutions/).
fun countVowels(text: String): Int {
    var count = 0
    for ((i, c) in text.withIndex()) {
        Drill.visit(i, 0)
        if (c in "aeiouAEIOU") { count += 1; Drill.write(0, count) }
    }
    return count
}
