// kind: WRONG_ALGORITHM
// first 를 훑으며 second 에서 다음에 나오는 같은 글자를 탐욕으로 짝짓는다. 뒤의 더 긴 짝을 놓친다.
fun lcsLength(first: String, second: String): Int {
    var j = 0
    var count = 0
    for (a in first) {
        var k = j
        while (k < second.length && second[k] != a) k += 1
        if (k < second.length) { count += 1; j = k + 1 }
    }
    return count
}
