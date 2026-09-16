// kind: WRONG_BRANCH
// 무리가 실제로 합쳐졌는지 보지 않고 기록 n-1 개째의 시각을 답한다.
fun earliestAllConnected(n: Int, logs: IntArray): Int {
    if (n == 1) return 0
    val m = logs.size / 3
    val order = (0 until m).sortedBy { logs[it * 3] }
    var merges = 0
    for (k in order) {
        merges += 1
        if (merges == n - 1) return logs[k * 3]
    }
    return -1
}
