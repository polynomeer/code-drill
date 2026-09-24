// kind: PERFORMANCE
// 좌표마다 모든 건물을 훑어 그 자리의 높이를 구한다. O(n^2).
fun skyline(buildings: IntArray): IntArray {
    val count = buildings.size / 3
    if (count == 0) return IntArray(0)
    val xs = java.util.TreeSet<Int>()
    for (i in 0 until count) { xs.add(buildings[3 * i]); xs.add(buildings[3 * i + 1]) }
    val out = ArrayList<Int>()
    var last = -1
    for (x in xs) {
        var best = 0
        for (i in 0 until count) {
            Drill.compare(x, i)
            if (buildings[3 * i] <= x && x < buildings[3 * i + 1] && buildings[3 * i + 2] > best) best = buildings[3 * i + 2]
        }
        if (best != last) { out.add(x); out.add(best); last = best }
    }
    return IntArray(out.size) { out[it] }
}
