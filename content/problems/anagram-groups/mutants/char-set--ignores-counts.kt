// kind: WRONG_ALGORITHM
// 글자의 집합을 이름표로 쓴다. 같은 글자가 몇 번인지 잊는다.
fun anagramGroups(words: Array<String>): Int = words.map { it.toSet() }.toSet().size
