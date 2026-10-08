// kind: WRONG_BRANCH
// y 도 모음으로 센다. 모음은 a·e·i·o·u 다섯뿐이다.
fun maxVowelsWindow(s: String, k: Int): Int {
    fun vowel(c: Char) = c in "aeiouy"
    var count = 0
    for (i in 0 until k) if (vowel(s[i])) count += 1
    var best = count
    for (i in k until s.length) { if (vowel(s[i])) count += 1; if (vowel(s[i - k])) count -= 1; if (count > best) best = count }
    return best
}
