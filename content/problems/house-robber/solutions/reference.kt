// 검증용 정답 (§6.1 solutions/). 두 상태만 들고 가는 DP.
//
// 각 집에서 갈리는 것은 "이 집을 고르는가"뿐이다. 고르면 직전을 못 고르므로 그 전까지의
// 최선에 더하고, 안 고르면 직전까지의 최선을 그대로 이어받는다. 배열을 따로 두지 않고
// 두 값만 들고 가면 메모리도 상수다.
fun rob(values: IntArray): Int {
    var skip = 0
    var take = 0

    for (index in values.indices) {
        Drill.visit(index, values[index])
        val nextSkip = maxOf(skip, take)
        val nextTake = skip + values[index]
        skip = nextSkip
        take = nextTake
        Drill.write(index, maxOf(skip, take))
    }
    return maxOf(skip, take)
}
