// 검증용 정답 (§6.1 solutions/). 창을 밀며 들어온 글자를 더하고 나간 글자를 뺀다.
fun maxVowelsWindow(s: String, k: Int): Int {
    fun vowel(c: Char) = c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u'
    var count = 0
    for (i in 0 until k) if (vowel(s[i])) count += 1
    var best = count
    for (i in k until s.length) {
        if (vowel(s[i])) count += 1
        if (vowel(s[i - k])) count -= 1
        Drill.write(i, count)
        if (count > best) best = count
    }
    return best
}
