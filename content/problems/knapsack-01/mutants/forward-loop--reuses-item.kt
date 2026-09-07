// kind: WRONG_BRANCH
// 용량을 작은 쪽부터 훑어 같은 물건을 여러 번 담는다.
fun knapsack(weights: IntArray, values: IntArray, capacity: Int): Int {
    val best = IntArray(capacity + 1)
    for (item in weights.indices) {
        for (room in weights[item]..capacity) {
            val candidate = best[room - weights[item]] + values[item]
            if (candidate > best[room]) best[room] = candidate
        }
    }
    return best[capacity]
}
