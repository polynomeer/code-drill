// 검증용 정답 (§6.1 solutions/). 1 이 하나뿐인가.
fun isPowerOfTwo(n: Int): Int {
    Drill.compare(n, n - 1)
    return if (n > 0 && n and (n - 1) == 0) 1 else 0
}
