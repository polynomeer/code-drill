// kind: WRONG_BRANCH
// 10 의 배수를 센다. 2 × 5 처럼 따로 떨어진 인수를 놓친다.
fun trailingZeros(n: Int): Int {
    var count = 0
    var power = 10L
    while (power <= n) { count += (n / power).toInt(); power *= 10 }
    return count
}
