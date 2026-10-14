// kind: WRONG_ALGORITHM
// 매 학기 들을 수 있는 과목 가운데 번호가 작은 k 개를 듣는다. 어느 과목을 먼저 듣느냐가 뒤를 바꾼다.
fun coursesWithLimit(n: Int, edges: IntArray, k: Int): Int {
    val need = IntArray(n)
    for (i in edges.indices step 2) need[edges[i + 1]] = need[edges[i + 1]] or (1 shl edges[i])
    var mask = 0; var terms = 0
    while (mask != (1 shl n) - 1) {
        var taken = 0; var next = mask
        for (v in 0 until n) if (taken < k && mask shr v and 1 == 0 && need[v] and mask == need[v]) { next = next or (1 shl v); taken += 1 }
        mask = next; terms += 1
    }
    return terms
}
