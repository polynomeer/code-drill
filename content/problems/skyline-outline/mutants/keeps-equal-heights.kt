// kind: MISSING_EDGE_CASE
// 직전 높이와 같아도 점을 찍는다. 윤곽선은 높이가 바뀌는 자리만 담는다.
fun skyline(buildings: IntArray): IntArray {
    val count = buildings.size / 3
    if (count == 0) return IntArray(0)
    fun merge(left: ArrayList<IntArray>, right: ArrayList<IntArray>): ArrayList<IntArray> {
        val out = ArrayList<IntArray>()
        var i = 0; var j = 0; var hl = 0; var hr = 0
        while (i < left.size && j < right.size) {
            val x: Int
            if (left[i][0] < right[j][0]) { x = left[i][0]; hl = left[i][1]; i += 1 }
            else if (left[i][0] > right[j][0]) { x = right[j][0]; hr = right[j][1]; j += 1 }
            else { x = left[i][0]; hl = left[i][1]; hr = right[j][1]; i += 1; j += 1 }
            out.add(intArrayOf(x, if (hl > hr) hl else hr))
        }
        while (i < left.size) { out.add(left[i]); i += 1 }
        while (j < right.size) { out.add(right[j]); j += 1 }
        return out
    }
    fun solve(lo: Int, hi: Int): ArrayList<IntArray> {
        if (lo == hi) {
            val one = ArrayList<IntArray>(2)
            one.add(intArrayOf(buildings[3 * lo], buildings[3 * lo + 2]))
            one.add(intArrayOf(buildings[3 * lo + 1], 0))
            return one
        }
        val mid = (lo + hi) / 2
        return merge(solve(lo, mid), solve(mid + 1, hi))
    }
    val points = solve(0, count - 1)
    val out = IntArray(points.size * 2)
    for (k in points.indices) { out[2 * k] = points[k][0]; out[2 * k + 1] = points[k][1] }
    return out
}
