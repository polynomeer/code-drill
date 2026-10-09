// kind: PERFORMANCE
// 시작마다 끝을 늘려 가며 센다. 맞지만, 30 만 글자면 450 억 쌍이다.
fun longestKRepeating(s: String, k: Int): Int {
    var best = 0
    for (start in s.indices) {
        val counts = IntArray(26)
        var short = 0
        for (end in start until s.length) {
            val c = s[end] - 'a'
            counts[c] += 1
            if (counts[c] == 1) short += 1
            if (counts[c] == k) short -= 1
            if (short == 0 && end - start + 1 > best) best = end - start + 1
        }
    }
    return best
}
