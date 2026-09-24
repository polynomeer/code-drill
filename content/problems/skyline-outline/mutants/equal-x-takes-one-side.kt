// kind: WRONG_BRANCH
// 두 윤곽선의 x 가 같을 때 한쪽만 꺼낸다. 다른 쪽의 높이 변화가 한 점 늦게 반영된다.
fun skyline(buildings: IntArray): IntArray {
    val count = buildings.size / 3
    if (count == 0) return IntArray(0)
    fun merge(left: ArrayList<IntArray>, right: ArrayList<IntArray>): ArrayList<IntArray> {
        val out = ArrayList<IntArray>()
        var i = 0; var j = 0; var hl = 0; var hr = 0
        fun push(x: Int, h: Int) { if (out.isEmpty() || out[out.size - 1][1] != h) out.add(intArrayOf(x, h)) }
        while (i < left.size && j < right.size) {
            val x: Int
            if (left[i][0] <= right[j][0]) { x = left[i][0]; hl = left[i][1]; i += 1 }
            else { x = right[j][0]; hr = right[j][1]; j += 1 }
            push(x, if (hl > hr) hl else hr)
        }
        while (i < left.size) { push(left[i][0], left[i][1]); i += 1 }
        while (j < right.size) { push(right[j][0], right[j][1]); j += 1 }
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
