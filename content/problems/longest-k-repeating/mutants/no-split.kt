// kind: WRONG_ALGORITHM
// 문자열 전체만 보고, 모자란 글자가 있으면 0 을 낸다. 모자란 글자를 피한 조각 안에 답이 있을 수 있다.
fun longestKRepeating(s: String, k: Int): Int {
    val counts = IntArray(26)
    for (c in s) counts[c - 'a'] += 1
    return if (s.all { counts[it - 'a'] >= k }) s.length else 0
}
