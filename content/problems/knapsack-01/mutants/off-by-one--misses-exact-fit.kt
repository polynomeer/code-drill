// kind: OFF_BY_ONE
// 용량과 정확히 맞는 경우를 건너뛴다.
fun knapsack(weights: IntArray, values: IntArray, capacity: Int): Int {
    val best = IntArray(capacity + 1)
    for (item in weights.indices) {
        for (room in capacity downTo weights[item] + 1) {
            val candidate = best[room - weights[item]] + values[item]
            if (candidate > best[room]) best[room] = candidate
        }
    }
    return best[capacity]
}
