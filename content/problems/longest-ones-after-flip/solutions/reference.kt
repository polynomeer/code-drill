// 검증용 정답 (§6.1 solutions/). 슬라이딩 윈도우.
//
// 창이 품은 0 의 개수가 k 를 넘으면 왼쪽을 민다. 왼쪽 끝이 되돌아가지 않으므로 두
// 포인터를 합쳐도 배열을 한 번 훑는 것과 같다.
//
// 답을 매번 갱신하는 것이 아니라 "지금 창의 길이"로 재는 것이 요령이다. 창은 절대
// 줄어들지 않으므로 마지막 길이가 곧 최댓값이 되는 구현도 가능하지만, 여기서는
// 읽기 쉬운 쪽을 골랐다.
fun longestOnes(bits: IntArray, k: Int): Int {
    var left = 0
    var zeros = 0
    var best = 0

    for (right in bits.indices) {
        if (bits[right] == 0) zeros += 1
        Drill.pointer("right", right)

        while (zeros > k) {
            if (bits[left] == 0) zeros -= 1
            left += 1
            Drill.pointer("left", left)
        }

        val length = right - left + 1
        if (length > best) {
            best = length
            Drill.write(0, best)
        }
    }
    return best
}
