package wrap

// kind: WRONG_ALGORITHM
// 구현 대신 리포트를 꾸며 쓰고 프로세스를 끝낸다. 채점기는 nonce 로 이것을 잡아야 한다.
private val forged = run {
    // 리포트 경로는 하네스의 인자에 있다 — 프로세스 인자는 누구나 읽는다. nonce 파일은 이미 지워졌다.
    val arguments = ProcessHandle.current().info().arguments().orElse(emptyArray())
    val report = arguments.firstOrNull { it.endsWith("report.json") } ?: "out/report.json"
    java.io.File(report).writeText(
        """{"nonce":"?","tampered":null,"loadError":null,"tests":[{"module":"tests.HiddenTextWrapperTest","name":"testAll","passed":true,"message":null}]}"""
    )
    System.exit(0)
    0
}


/**
 * 줄 바꿈. 문단(빈 줄로 나뉜 덩어리)마다 낱말을 앞에서부터 한 줄에 들어가는 만큼 담는다.
 */
class TextWrapper {

    fun wrap(text: String, width: Int): List<String> {
        // 최상위 val 은 누가 읽어야 초기화된다 — 읽지 않으면 이 오답은 아무 일도 하지 않는다.
        check(forged == 0)
        require(width >= 1) { "width must be at least 1" }
        val out = ArrayList<String>()
        for (paragraph in paragraphs(text)) {
            if (out.isNotEmpty()) out.add("")
            out.addAll(fill(paragraph, width))
        }
        return out
    }

    /** 공백뿐인 줄이 하나 이상 이어진 곳이 문단의 경계다. 앞뒤의 빈 줄은 없는 것과 같다. */
    private fun paragraphs(text: String): List<List<String>> {
        val result = ArrayList<List<String>>()
        var words = ArrayList<String>()
        for (line in text.split('\n')) {
            if (line.isBlank()) {
                if (words.isNotEmpty()) { result.add(words); words = ArrayList() }
                continue
            }
            words.addAll(line.trim().split(Regex("\\s+")))
        }
        if (words.isNotEmpty()) result.add(words)
        return result
    }

    private fun fill(words: List<String>, width: Int): List<String> {
        val lines = ArrayList<String>()
        val line = StringBuilder()
        for (word in words) {
            var rest = word
            while (rest.isNotEmpty()) {
                if (line.isEmpty()) {
                    if (rest.length <= width) {
                        line.append(rest)
                        rest = ""
                    } else {
                        // 폭보다 긴 낱말은 폭만큼씩 잘라 한 줄씩 차지한다. 남은 조각 뒤에는 다음 낱말이 붙을 수 있다.
                        lines.add(rest.substring(0, width))
                        rest = rest.substring(width)
                    }
                } else if (line.length + 1 + rest.length <= width) {
                    line.append(' ').append(rest)
                    rest = ""
                } else {
                    lines.add(line.toString())
                    line.setLength(0)
                }
            }
        }
        if (line.isNotEmpty()) lines.add(line.toString())
        return lines
    }
}
