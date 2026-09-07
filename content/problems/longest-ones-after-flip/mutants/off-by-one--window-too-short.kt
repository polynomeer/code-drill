// kind: OFF_BY_ONE
// 창의 길이를 한 칸 짧게 센다.
fun longestOnes(bits: IntArray, k: Int): Int {
    var left = 0
    var zeros = 0
    var best = 0
    for (right in bits.indices) {
        if (bits[right] == 0) zeros += 1
        while (zeros > k) {
            if (bits[left] == 0) zeros -= 1
            left += 1
        }
        val length = right - left
        if (length > best) best = length
    }
    return best
}
