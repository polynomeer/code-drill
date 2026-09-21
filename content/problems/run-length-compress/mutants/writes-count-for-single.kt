// kind: OFF_BY_ONE
// 한 번 나온 글자에도 1 을 붙인다.
fun runLengthCompress(text: String): String {
    val chars = text.toCharArray()
    var read = 0; var write = 0
    while (read < chars.size) {
        val ch = chars[read]; var end = read
        while (end < chars.size && chars[end] == ch) end += 1
        chars[write++] = ch
        for (d in (end - read).toString()) chars[write++] = d
        read = end
    }
    return String(chars, 0, write)
}
