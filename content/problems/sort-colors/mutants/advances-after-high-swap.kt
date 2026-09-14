// kind: WRONG_BRANCH
// 2 를 오른쪽으로 보낸 뒤 가운데 포인터도 옮긴다. 오른쪽에서 온 값을 보지 않고 지나친다.
fun sortColors(nums: IntArray): IntArray {
    val out = nums.copyOf()
    var low = 0; var mid = 0; var high = out.size - 1
    while (mid <= high) {
        when (out[mid]) {
            0 -> { val t = out[low]; out[low] = out[mid]; out[mid] = t; low += 1; mid += 1 }
            2 -> { val t = out[high]; out[high] = out[mid]; out[mid] = t; high -= 1; mid += 1 }
            else -> mid += 1
        }
    }
    return out
}
