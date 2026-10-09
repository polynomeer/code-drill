// kind: MISSING_EDGE_CASE
// 새 점을 덱에 넣은 뒤에 답을 갱신한다. 점이 자기 자신과 짝지어져 2·ys[j] 가 답이 된다.
fun maxEquationValue(xs: IntArray, ys: IntArray, k: Int): Int {
    val window = IntArray(xs.size); var head = 0; var tail = 0
    var best = Long.MIN_VALUE
    for (j in xs.indices) {
        while (head < tail && xs[j].toLong() - xs[window[head]] > k) head += 1
        while (head < tail && ys[window[tail - 1]] - xs[window[tail - 1]] <= ys[j] - xs[j]) tail -= 1
        window[tail++] = j
        val i = window[head]
        best = maxOf(best, ys[i].toLong() - xs[i] + ys[j] + xs[j])
    }
    return best.toInt()
}
