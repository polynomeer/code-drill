// kind: WRONG_ALGORITHM
// 쌍마다 한 번씩, 나아질 때만 맞바꾼다. 맞바꾸기를 이어 붙여야 닿는 자리가 있다.
fun smallestStringWithSwaps(s: String, pairs: IntArray): String {
    val out = s.toCharArray()
    for (i in pairs.indices step 2) {
        val a = minOf(pairs[i], pairs[i + 1]); val b = maxOf(pairs[i], pairs[i + 1])
        if (out[b] < out[a]) { val t = out[a]; out[a] = out[b]; out[b] = t }
    }
    return String(out)
}
