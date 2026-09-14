// kind: OFF_BY_ONE
// 가운데 포인터가 오른쪽 경계와 같아지면 멈춘다. 마지막 칸을 보지 않는다.
fun sortColors(nums: IntArray): IntArray {
    val out = nums.copyOf()
    var low = 0; var mid = 0; var high = out.size - 1
    while (mid < high) {
        when (out[mid]) {
            0 -> { val t = out[low]; out[low] = out[mid]; out[mid] = t; low += 1; mid += 1 }
            2 -> { val t = out[high]; out[high] = out[mid]; out[mid] = t; high -= 1 }
            else -> mid += 1
        }
    }
    return out
}
