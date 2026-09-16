// kind: PERFORMANCE
// 힙 없이 넣은 작업들을 목록에 두고 넘칠 때마다 훑어 가장 긴 것을 찾는다. O(n²).
fun maxTasksBeforeDeadlines(durations: IntArray, deadlines: IntArray): Int {
    val order = durations.indices.sortedBy { deadlines[it] }
    val chosen = ArrayList<Int>()
    var elapsed = 0L
    for (i in order) {
        elapsed += durations[i]; chosen.add(durations[i])
        if (elapsed > deadlines[i]) {
            var longest = 0
            for (k in chosen.indices) { Drill.compare(chosen[k], chosen[longest]); if (chosen[k] > chosen[longest]) longest = k }
            elapsed -= chosen[longest]
            chosen.removeAt(longest)
        }
    }
    return chosen.size
}
