// 검증용 정답 (§6.1 solutions/). 값의 범위가 작으니 배열이 빈도표다.
fun mostFrequent(nums: IntArray): Int {
    val counts = IntArray(101)
    for (v in nums) {
        counts[v] += 1
        Drill.write(v, counts[v])
    }
    var best = 0
    for (v in 1..100) {
        Drill.compare(v, best)
        if (counts[v] > counts[best]) best = v
    }
    return best
}
