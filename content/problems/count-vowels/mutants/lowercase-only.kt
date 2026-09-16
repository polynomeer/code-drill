// kind: MISSING_EDGE_CASE
// 소문자 모음만 센다.
fun countVowels(text: String): Int = text.count { it in "aeiou" }
