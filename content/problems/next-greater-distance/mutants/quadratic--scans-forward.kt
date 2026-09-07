// kind: PERFORMANCE
// 각 위치에서 앞을 끝까지 훑어 O(n^2) 다. 내림차순 입력이 최악이다.
fun nextGreater(values: IntArray): IntArray {
    val out = IntArray(values.size)
    for (index in values.indices) {
        for (next in index + 1 until values.size) {
            if (values[next] > values[index]) {
                out[index] = next - index
                break
            }
        }
    }
    return out
}
