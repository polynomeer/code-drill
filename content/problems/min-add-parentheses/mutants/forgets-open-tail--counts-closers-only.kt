// kind: MISSING_EDGE_CASE
// 끝에 열린 채 남은 괄호를 세지 않는다.
fun minAdd(text: String): Int {
    var open = 0; var insertions = 0
    for (ch in text) {
        if (ch == '(') open += 1 else if (open > 0) open -= 1 else insertions += 1
    }
    return insertions
}
