// kind: WRONG_ALGORITHM
// 여는 수와 닫는 수의 차이만 본다. `)(` 는 차이가 0 이지만 두 개를 끼워야 한다.
fun minAdd(text: String): Int {
    var open = 0; var close = 0
    for (ch in text) if (ch == '(') open += 1 else close += 1
    return if (open > close) open - close else close - open
}
