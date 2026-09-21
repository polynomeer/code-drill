// kind: WRONG_ALGORITHM
// 공백으로 나눠 다시 합친다. 여러 공백과 앞뒤 공백이 사라진다.
fun titleCase(text: String): String =
    text.split(" ").filter { it.isNotEmpty() }.joinToString(" ") { it.lowercase().replaceFirstChar { c -> c.uppercaseChar() } }
