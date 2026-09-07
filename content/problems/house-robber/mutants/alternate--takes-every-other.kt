// kind: WRONG_BRANCH
// 한 칸 건너 전부 고른다. 양 끝을 고르는 것이 나은 경우를 놓친다.
fun rob(values: IntArray): Int {
    var even = 0
    var odd = 0
    for (index in values.indices) {
        if (index % 2 == 0) even += values[index] else odd += values[index]
    }
    return maxOf(even, odd)
}
