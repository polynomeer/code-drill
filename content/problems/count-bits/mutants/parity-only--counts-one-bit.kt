// kind: WRONG_ALGORITHM
// 마지막 비트만 본다. 3 이상에서 갈린다.
fun countBits(n: Int): IntArray = IntArray(n + 1) { it and 1 }
