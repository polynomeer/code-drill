// kind: WRONG_ALGORITHM
// 바로 앞의 점과만 짝짓는다. 창 안의 더 먼 점이 나을 수 있다.
fun maxEquationValue(xs: IntArray, ys: IntArray, k: Int): Int {
    var best = Long.MIN_VALUE
    for (j in 1 until xs.size) if (xs[j].toLong() - xs[j - 1] <= k) best = maxOf(best, ys[j - 1].toLong() + ys[j] + xs[j] - xs[j - 1])
    return best.toInt()
}
