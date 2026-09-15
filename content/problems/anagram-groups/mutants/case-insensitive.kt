// kind: MISSING_EDGE_CASE
// 대소문자를 같게 본다.
fun anagramGroups(words: Array<String>): Int =
    words.map { String(it.lowercase().toCharArray().sortedArray()) }.toSet().size
