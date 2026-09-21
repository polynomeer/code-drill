// kind: WRONG_ALGORITHM
// 세 번 이상 나온 값은 아예 지운다.
fun removeDuplicatesAtMostTwice(nums: IntArray): IntArray {
    val out = ArrayList<Int>()
    var i = 0
    while (i < nums.size) {
        var j = i
        while (j < nums.size && nums[j] == nums[i]) j += 1
        if (j - i <= 2) for (t in i until j) out.add(nums[t])
        i = j
    }
    return out.toIntArray()
}
