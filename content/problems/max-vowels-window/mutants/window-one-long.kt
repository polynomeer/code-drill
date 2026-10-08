// kind: OFF_BY_ONE
// 나가는 글자를 한 칸 늦게 뺀다. 창이 k + 1 글자가 된다.
fun maxVowelsWindow(s: String, k: Int): Int {
    fun vowel(c: Char) = c in "aeiou"
    var count = 0
    for (i in 0 until k) if (vowel(s[i])) count += 1
    var best = count
    for (i in k until s.length) { if (vowel(s[i])) count += 1; if (i - k - 1 >= 0 && vowel(s[i - k - 1])) count -= 1; if (count > best) best = count }
    return best
}
