// 검증용 정답 (§6.1 solutions/). 두 줄만 들고 가는 DP.
//
// 세 연산이 각각 격자의 한 방향에 대응한다. 위에서 오면 삭제, 왼쪽에서 오면 삽입,
// 대각선에서 오면 교체(같으면 공짜)다. 대각선을 빠뜨리면 교체가 두 번으로 세어진다.
fun editDistance(source: IntArray, target: IntArray): Int {
    var previous = IntArray(target.size + 1) { it }
    val current = IntArray(target.size + 1)

    for (i in 1..source.size) {
        current[0] = i
        for (j in 1..target.size) {
            Drill.compare(i - 1, j - 1)
            val cost = if (source[i - 1] == target[j - 1]) 0 else 1
            current[j] = minOf(
                previous[j] + 1,
                current[j - 1] + 1,
                previous[j - 1] + cost,
            )
            Drill.write(j, current[j])
        }
        previous = current.copyOf()
    }
    return previous[target.size]
}
