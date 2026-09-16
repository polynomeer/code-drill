// kind: PERFORMANCE
// 지울 때마다 문자열 전체를 훑어 내려가는 첫 자리를 찾는다. O(n·k).
fun removeKDigits(num: String, k: Int): String {
    val digits = StringBuilder(num)
    // 훑으며 내림 자리의 수까지 센다 — 이 값을 쓰지 않으면 JIT 가 빈 순회를 통째로 지운다.
    var descents = 0L
    repeat(k) {
        var first = -1
        for (i in 0 until digits.length - 1) {
            Drill.compare(digits[i] - '0', digits[i + 1] - '0')
            if (digits[i] > digits[i + 1]) { descents += 1; if (first == -1) first = i }
        }
        if (digits.isNotEmpty()) digits.deleteCharAt(if (first == -1) digits.length - 1 else first)
    }
    val trimmed = digits.toString().trimStart('0')
    return if (descents < 0) "!" else if (trimmed.isEmpty()) "0" else trimmed
}
