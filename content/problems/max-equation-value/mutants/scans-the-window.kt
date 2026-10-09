// kind: PERFORMANCE
// 점마다 창 안의 앞선 점을 모두 훑는다. 맞지만, 창이 넓으면 n² 이다.
fun maxEquationValue(xs: IntArray, ys: IntArray, k: Int): Int {
    var best = Long.MIN_VALUE
    for (j in xs.indices) {
        var i = j - 1
        while (i >= 0 && xs[j].toLong() - xs[i] <= k) { best = maxOf(best, ys[i].toLong() - xs[i] + ys[j] + xs[j]); i -= 1 }
    }
    return best.toInt()
}
