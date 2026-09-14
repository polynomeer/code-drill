// kind: WRONG_BRANCH
// 창 밖에 있던 글자를 다시 만나도 왼쪽 끝을 그 자리로 되돌린다. 창이 다시 넓어진다.
fun longestUnique(text: String): Int {
    val last = HashMap<Char, Int>()
    var start = 0
    var best = 0
    for (i in text.indices) {
        val seen = last[text[i]]
        if (seen != null) start = seen + 1
        last[text[i]] = i
        best = maxOf(best, i - start + 1)
    }
    return best
}
