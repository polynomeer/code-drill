// kind: MISSING_EDGE_CASE
// 길이를 한 자리로만 쓴다. 10 이상이 깨진다.
fun runLengthCompress(text: String): String {
    val chars = text.toCharArray()
    var read = 0; var write = 0
    while (read < chars.size) {
        val ch = chars[read]; var end = read
        while (end < chars.size && chars[end] == ch) end += 1
        chars[write++] = ch
        if (end - read > 1) chars[write++] = ('0' + (end - read) % 10)
        read = end
    }
    return String(chars, 0, write)
}
