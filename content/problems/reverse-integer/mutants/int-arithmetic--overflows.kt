// kind: MISSING_EDGE_CASE
// Int 로 뒤집고 범위를 보지 않는다. 넘치는 입력에서 엉뚱한 수를 낸다.
fun reverseInteger(n: Int): Int {
    var rest = n
    var result = 0
    while (rest != 0) { result = result * 10 + rest % 10; rest /= 10 }
    return result
}
