// kind: PERFORMANCE
// 가운데마다 양쪽을 다 훑어 센다. 맞지만, 20 만이면 400 억 번의 비교다.
fun increasingTripletsCount(nums: IntArray): Int {
    val n = nums.size
    var total = 0L
    for (j in 0 until n) {
        var left = 0L; var right = 0L
        for (i in 0 until j) if (nums[i] < nums[j]) left += 1
        for (k in j + 1 until n) if (nums[k] > nums[j]) right += 1
        total = (total + left * right) % 1_000_000_007
    }
    return total.toInt()
}
