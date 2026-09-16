// kind: OFF_BY_ONE
// 뒤 절반만 뒤집어 앞 절반과 견주되, 홀수 자리의 가운데를 떼지 않는다.
fun isPalindromeNumber(n: Int): Int {
    if (n < 0) return 0
    if (n % 10 == 0 && n != 0) return 0
    var rest = n; var reversed = 0
    while (rest > reversed) { reversed = reversed * 10 + rest % 10; rest /= 10 }
    return if (rest == reversed) 1 else 0
}
