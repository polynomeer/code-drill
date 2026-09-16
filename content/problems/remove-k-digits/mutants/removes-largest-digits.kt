// kind: WRONG_ALGORITHM
// 값이 가장 큰 자리 k 개를 지운다. 자리의 위치가 값보다 중요하다.
fun removeKDigits(num: String, k: Int): String {
    val keep = BooleanArray(num.length) { true }
    var left = k
    var digit = '9'
    while (left > 0 && digit >= '0') {
        for (i in num.indices) if (left > 0 && keep[i] && num[i] == digit) { keep[i] = false; left -= 1 }
        digit -= 1
    }
    val out = StringBuilder()
    for (i in num.indices) if (keep[i]) out.append(num[i])
    val trimmed = out.toString().trimStart('0')
    return if (trimmed.isEmpty()) "0" else trimmed
}
