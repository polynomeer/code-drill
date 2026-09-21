// kind: OFF_BY_ONE
// 마지막 구간을 쓰지 않는다 — 구간의 끝을 다음 글자가 바뀔 때만 감지한다.
fun runLengthCompress(text: String): String {
    val out = StringBuilder()
    var run = 1
    for (i in 1 until text.length) {
        if (text[i] == text[i - 1]) { run += 1; continue }
        out.append(text[i - 1]); if (run > 1) out.append(run)
        run = 1
    }
    return out.toString()
}
