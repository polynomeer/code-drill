// kind: WRONG_BRANCH
// 가치 밀도가 높은 것부터 욕심껏 담는다.
fun knapsack(weights: IntArray, values: IntArray, capacity: Int): Int {
    val order = weights.indices.sortedByDescending { values[it].toDouble() / weights[it] }
    var room = capacity
    var total = 0
    for (item in order) {
        if (weights[item] <= room) {
            room -= weights[item]
            total += values[item]
        }
    }
    return total
}
