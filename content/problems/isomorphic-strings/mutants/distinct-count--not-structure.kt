// kind: WRONG_ALGORITHM
// 서로 다른 문자의 개수만 비교한다. 개수가 같아도 자리가 다르면 다른 모양이다.
fun isomorphic(first: String, second: String): Int {
    if (first.length != second.length) return 0
    return if (first.toSet().size == second.toSet().size) 1 else 0
}
