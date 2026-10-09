// kind: OFF_BY_ONE
// x 의 차가 k 와 같은 점도 창에서 뺀다. 차가 정확히 k 인 쌍도 된다.
fun maxEquationValue(xs: IntArray, ys: IntArray, k: Int): Int {
    val window = IntArray(xs.size); var head = 0; var tail = 0
    var best = Long.MIN_VALUE
    for (j in xs.indices) {
        while (head < tail && xs[j].toLong() - xs[window[head]] >= k) head += 1
        if (head < tail) { val i = window[head]; best = maxOf(best, ys[i].toLong() - xs[i] + ys[j] + xs[j]) }
        while (head < tail && ys[window[tail - 1]] - xs[window[tail - 1]] <= ys[j] - xs[j]) tail -= 1
        window[tail++] = j
    }
    return best.toInt()
}
