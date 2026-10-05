// kind: MISSING_EDGE_CASE
// 창이 배열 끝에서 멈춘다. 원형이라 끝 다음은 처음이다.
fun defuseCode(code: IntArray, k: Int): IntArray {
    val n = code.size
    return IntArray(n) { i ->
        var sum = 0
        if (k > 0) { for (j in i + 1..i + k) if (j < n) sum += code[j] }
        else if (k < 0) { for (j in i + k until i) if (j >= 0) sum += code[j] }
        sum
    }
}
