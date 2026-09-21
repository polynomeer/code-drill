// 검증용 정답 (§6.1 solutions/). 읽는 자리와 쓰는 자리 둘로 제자리.
fun runLengthCompress(text: String): String {
    val chars = text.toCharArray()
    var read = 0
    var write = 0
    while (read < chars.size) {
        val ch = chars[read]
        var end = read
        while (end < chars.size && chars[end] == ch) end += 1
        Drill.compare(read, write)
        chars[write++] = ch
        Drill.write(write - 1, ch.code)
        if (end - read > 1) for (d in (end - read).toString()) { chars[write++] = d; Drill.write(write - 1, d.code) }
        read = end
    }
    return String(chars, 0, write)
}
