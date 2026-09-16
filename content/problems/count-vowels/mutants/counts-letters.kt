// kind: WRONG_ALGORITHM
// 영문자 전부를 센다.
fun countVowels(text: String): Int = text.count { it in 'a'..'z' || it in 'A'..'Z' }
