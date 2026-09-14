// kind: PERFORMANCE
// 학기마다 모든 관계를 다시 훑어 들을 수 있는 과목을 찾는다. O(학기 × 관계).
fun minSemesters(n: Int, prereqs: IntArray): Int {
    val taken = BooleanArray(n)
    var count = 0
    var semesters = 0
    while (count < n) {
        val ready = mutableListOf<Int>()
        for (v in 0 until n) {
            if (taken[v]) continue
            var ok = true
            var i = 0
            while (i < prereqs.size) {
                Drill.compare(v, i / 2)
                if (prereqs[i + 1] == v && !taken[prereqs[i]]) { ok = false; break }
                i += 2
            }
            if (ok) ready.add(v)
        }
        if (ready.isEmpty()) return -1
        for (v in ready) taken[v] = true
        count += ready.size
        semesters += 1
    }
    return semesters
}
