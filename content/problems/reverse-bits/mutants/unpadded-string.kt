// kind: WRONG_ALGORITHM
// 이진 문자열을 앞자리 0 없이 뒤집는다. 32 자리로 채운 뒤 뒤집어야 한다.
fun reverseBits(n: Int): Int = Integer.toBinaryString(n).reversed().toLong(2).toInt()
