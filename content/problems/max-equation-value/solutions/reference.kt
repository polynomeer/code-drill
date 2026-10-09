// 검증용 정답 (§6.1 solutions/). ys - xs 가 줄어드는 덱. 창 밖은 앞에서 빼고, 답을 먼저 갱신한 뒤 새 점을 뒤에 넣는다.
fun maxEquationValue(xs: IntArray, ys: IntArray, k: Int): Int {
    val window = IntArray(xs.size)
    var head = 0
    var tail = 0
    var best = Long.MIN_VALUE
    for (j in xs.indices) {
        while (head < tail && xs[j].toLong() - xs[window[head]] > k) { Drill.dequeue(window[head]); head += 1 }
        if (head < tail) {
            val i = window[head]
            best = maxOf(best, ys[i].toLong() - xs[i] + ys[j] + xs[j])
        }
        while (head < tail && ys[window[tail - 1]] - xs[window[tail - 1]] <= ys[j] - xs[j]) tail -= 1
        window[tail++] = j
        Drill.enqueue(j)
    }
    return best.toInt()
}
