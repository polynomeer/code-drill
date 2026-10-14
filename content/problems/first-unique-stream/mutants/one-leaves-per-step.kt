// kind: WRONG_BRANCH
// 글자마다 큐 앞에서 많아야 하나만 뺀다. 앞의 여럿이 한꺼번에 외톨이가 아니게 될 수 있다.
fun firstUniqueStream(s: String): String {
    val counts = IntArray(26); val waiting = ArrayDeque<Char>(); val out = StringBuilder()
    for (c in s) {
        counts[c - 'a'] += 1; waiting.addLast(c)
        if (waiting.isNotEmpty() && counts[waiting.first() - 'a'] > 1) waiting.removeFirst()
        out.append(if (waiting.isEmpty()) '#' else waiting.first())
    }
    return out.toString()
}
