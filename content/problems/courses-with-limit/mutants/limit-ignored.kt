// kind: WRONG_BRANCH
// 한 학기에 들을 수 있는 과목을 모두 듣는다. 한 학기에 많아야 k 과목이다.
fun coursesWithLimit(n: Int, edges: IntArray, k: Int): Int {
    val need = IntArray(n)
    for (i in edges.indices step 2) need[edges[i + 1]] = need[edges[i + 1]] or (1 shl edges[i])
    var mask = 0; var terms = 0
    while (mask != (1 shl n) - 1) {
        var next = mask
        for (v in 0 until n) if (mask shr v and 1 == 0 && need[v] and mask == need[v]) next = next or (1 shl v)
        mask = next; terms += 1
    }
    return terms
}
