// kind: PERFORMANCE
// 창이 넘칠 때마다 왼쪽을 처음부터 다시 민다. 작은 입력은 통과한다.
fun longestOnes(bits: IntArray, k: Int): Int {
    var best = 0
    for (start in bits.indices) {
        var zeros = 0
        for (end in start until bits.size) {
            if (bits[end] == 0) zeros += 1
            if (zeros > k) break
            val length = end - start + 1
            if (length > best) best = length
        }
    }
    return best
}
