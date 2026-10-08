// kind: PERFORMANCE
// 창마다 모음을 처음부터 다시 센다. 길이 × 창 크기만큼 센다.
fun maxVowelsWindow(s: String, k: Int): Int {
    var best = 0
    for (start in 0..s.length - k) {
        var count = 0
        for (i in start until start + k) {
            val c = s[i]
            if (c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u') count += 1
        }
        Drill.write(start, count)
        if (count > best) best = count
    }
    return best
}
