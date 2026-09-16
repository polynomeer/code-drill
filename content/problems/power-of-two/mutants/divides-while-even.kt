// kind: OFF_BY_ONE
// 2 로 나누어떨어지는 동안 나누고 1 인지 본다 — 시작을 1 로 잡지 않아 1 이 아니라고 본다.
fun isPowerOfTwo(n: Int): Int {
    if (n <= 1) return 0
    var v = n
    while (v % 2 == 0) v /= 2
    return if (v == 1) 1 else 0
}
