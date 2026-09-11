// 검증용 정답 (§6.1 solutions/). 열린 수 하나로 센다.
fun minAdd(text: String): Int {
    var open = 0
    var insertions = 0
    for (i in text.indices) {
        if (text[i] == '(') {
            open += 1
            Drill.push(i)
        } else if (open > 0) {
            open -= 1
            Drill.pop(i)
        } else {
            insertions += 1
            Drill.write(i, insertions)
        }
    }
    return insertions + open
}
