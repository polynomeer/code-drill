// kind: MISSING_EDGE_CASE
// 0 을 막지 않는다. 0 & -1 = 0 이라 통과한다.
fun isPowerOfTwo(n: Int): Int = if (n and (n - 1) == 0) 1 else 0
