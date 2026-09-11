// kind: WRONG_BRANCH
// 열린 수를 음수로 내려가게 둔다. 뒤에 오는 여는 괄호가 앞의 빚을 갚는 것으로 잘못 센다.
fun minAdd(text: String): Int {
    var open = 0
    for (ch in text) if (ch == '(') open += 1 else open -= 1
    return if (open < 0) -open else open
}
