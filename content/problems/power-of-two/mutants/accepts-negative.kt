// kind: MISSING_EDGE_CASE
// 음수를 막지 않는다. Int.MIN_VALUE 는 1 이 하나라 통과한다.
fun isPowerOfTwo(n: Int): Int = if (n != 0 && n and (n - 1) == 0) 1 else 0
