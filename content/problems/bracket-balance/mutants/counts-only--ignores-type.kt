// kind: WRONG_BRANCH
// 여는 것과 닫는 것의 개수만 센다. 종류가 어긋난 경우를 통과시킨다.
fun isBalanced(tokens: IntArray): Int {
    var depth = 0
    for (token in tokens) {
        depth += if (token > 0) 1 else -1
        if (depth < 0) return 0
    }
    return if (depth == 0) 1 else 0
}
