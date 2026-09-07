// 검증용 정답 (§6.1 solutions/). 1차원 배열 DP.
//
// 용량을 **큰 쪽부터** 훑는다. 작은 쪽부터 훑으면 방금 이 물건을 넣어 갱신한 값을
// 다시 읽어, 같은 물건을 여러 번 담은 답이 나온다 — 그건 다른 문제(무한 배낭)의 답이다.
fun knapsack(weights: IntArray, values: IntArray, capacity: Int): Int {
    val best = IntArray(capacity + 1)

    for (item in weights.indices) {
        Drill.visit(item, values[item])
        val weight = weights[item]

        for (room in capacity downTo weight) {
            val candidate = best[room - weight] + values[item]
            if (candidate > best[room]) {
                best[room] = candidate
                Drill.write(room, candidate)
            }
        }
    }
    return best[capacity]
}
