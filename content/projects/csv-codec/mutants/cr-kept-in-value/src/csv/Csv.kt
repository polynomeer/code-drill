package csv

// kind: WRONG_BRANCH
// CRLF 의 \r 을 값의 일부로 둔다.

object Csv {
    fun parse(text: String): List<List<String>> {
        val rows = ArrayList<List<String>>()
        if (text.isEmpty()) return rows
        var i = 0
        val n = text.length
        var row = ArrayList<String>()
        val field = StringBuilder()
        while (true) {
            // 값 하나
            if (i < n && text[i] == '"') {
                i += 1
                while (true) {
                    if (i >= n) throw MalformedCsv(n, "unterminated quoted field")
                    val ch = text[i]
                    if (ch == '"') {
                        if (i + 1 < n && text[i + 1] == '"') { field.append('"'); i += 2 } else { i += 1; break }
                    } else { field.append(ch); i += 1 }
                }
                if (i < n && text[i] != ',' && text[i] != '\n' && text[i] != '\r') throw MalformedCsv(i, "text after closing quote")
            } else {
                while (i < n && text[i] != ',' && text[i] != '\n') {
                    if (text[i] == '"') throw MalformedCsv(i, "quote inside unquoted field")
                    field.append(text[i]); i += 1
                }
            }
            row.add(field.toString()); field.setLength(0)
            // 값 뒤: 쉼표면 다음 값, 줄바꿈이면 다음 줄, 끝이면 끝.
            if (i < n && text[i] == ',') { i += 1; continue }
            rows.add(row); row = ArrayList()
            if (i >= n) break
            if (text[i] == '\r') i += 1
            if (i < n && text[i] == '\n') i += 1
            if (i >= n) break   // 마지막 줄바꿈 하나는 없는 것과 같다.
        }
        return rows
    }

    fun format(rows: List<List<String>>): String {
        val out = StringBuilder()
        for (row in rows) {
            row.forEachIndexed { index, value ->
                if (index > 0) out.append(',')
                if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
                    out.append('"').append(value.replace("\"", "\"\"")).append('"')
                } else out.append(value)
            }
            out.append('\n')
        }
        return out.toString()
    }
}
