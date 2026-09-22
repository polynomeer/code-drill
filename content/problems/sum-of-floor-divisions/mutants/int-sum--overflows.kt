// kind: WRONG_BRANCH
// 합을 Int 로 더하고 끝에서 나머지를 취한다. 10⁸ 에서 넘친다.
fun sumOfFloorDivisions(n: Int): Int {
    var total = 0
    var i = 1L
    while (i <= n) {
        val q = n / i
        val j = n / q
        total += (q * (j - i + 1)).toInt()
        i = j + 1
    }
    return total % 1_000_000_007
}
