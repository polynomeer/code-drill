// kind: WRONG_ALGORITHM
// 부동소수점 제곱근을 내림한다. 큰 수에서 반올림이 어긋난다 — 그리고 문제가 쓰지 말라고 했다.
fun isqrt(n: Int): Int = Math.sqrt(n.toFloat().toDouble()).toInt()
