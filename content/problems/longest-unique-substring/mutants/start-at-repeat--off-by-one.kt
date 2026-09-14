// kind: OFF_BY_ONE
// 왼쪽 끝을 반복된 글자의 다음이 아니라 그 자리로 당긴다. 같은 글자 둘이 창에 남는다.
fun longestUnique(text: String): Int {
    val last = HashMap<Char, Int>()
    var start = 0
    var best = 0
    for (i in text.indices) {
        val seen = last[text[i]]
        if (seen != null && seen >= start) start = seen
        last[text[i]] = i
        best = maxOf(best, i - start + 1)
    }
    return best
}
