// kind: WRONG_BRANCH
// 한 번 자른 조각을 다시 나누지 않고, 조각 안에서 모자란 글자가 없을 때만 답으로 본다. 자른 뒤 조각 안에서 새로 모자란 글자가 생긴다.
fun longestKRepeating(s: String, k: Int): Int {
    val counts = IntArray(26)
    for (c in s) counts[c - 'a'] += 1
    var best = 0
    var start = 0
    for (i in 0..s.length) {
        if (i == s.length || counts[s[i] - 'a'] < k) {
            val piece = IntArray(26)
            for (j in start until i) piece[s[j] - 'a'] += 1
            if (i > start && (start until i).all { piece[s[it] - 'a'] >= k }) best = maxOf(best, i - start)
            start = i + 1
        }
    }
    return best
}
