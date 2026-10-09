// kind: WRONG_BRANCH
// 덱을 ys 로만 줄 세운다. 짝의 몫은 ys[i] - xs[i] 라 x 가 앞선 점이 그만큼 손해다.
fun maxEquationValue(xs: IntArray, ys: IntArray, k: Int): Int {
    val window = IntArray(xs.size); var head = 0; var tail = 0
    var best = Long.MIN_VALUE
    for (j in xs.indices) {
        while (head < tail && xs[j].toLong() - xs[window[head]] > k) head += 1
        if (head < tail) { val i = window[head]; best = maxOf(best, ys[i].toLong() - xs[i] + ys[j] + xs[j]) }
        while (head < tail && ys[window[tail - 1]] <= ys[j]) tail -= 1
        window[tail++] = j
    }
    return best.toInt()
}
