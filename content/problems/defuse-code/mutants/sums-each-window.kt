// kind: PERFORMANCE
// 자리마다 창을 처음부터 다시 더한다. 창이 넓으면 n × |k| 번 더한다.
fun defuseCode(code: IntArray, k: Int): IntArray {
    val n = code.size
    return IntArray(n) { i ->
        var sum = 0
        if (k > 0) for (j in 1..k) sum += code[(i + j) % n]
        else if (k < 0) for (j in 1..-k) sum += code[(i - j + n) % n]
        Drill.write(i, sum)
        sum
    }
}
