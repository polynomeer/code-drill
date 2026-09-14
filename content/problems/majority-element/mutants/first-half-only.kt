// kind: WRONG_ALGORITHM
// 앞 절반만 보고 가장 많이 나온 값을 답한다. 과반수가 뒤에 몰려 있으면 틀린다.
fun majority(nums: IntArray): Int {
    val counts = HashMap<Int, Int>()
    for (i in 0..nums.size / 2) counts[nums[i]] = (counts[nums[i]] ?: 0) + 1
    return counts.maxByOrNull { it.value }!!.key
}
