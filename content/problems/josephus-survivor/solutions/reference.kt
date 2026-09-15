// 검증용 정답 (§6.1 solutions/). n-1 명의 답에서 n 명의 답으로.
fun survivor(n: Int, k: Int): Int {
    var result = 0
    for (size in 2..n) {
        result = (result + k) % size
        Drill.write(size, result)
    }
    return result + 1
}
