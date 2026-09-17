// kind: WRONG_ALGORITHM
// 3 × (서로 다른 수의 합) − 전체 합 을 2 로 나눈다. Long 없이 넘친다.
fun singleNumberThrice(nums: IntArray): Int {
    val distinct = HashSet<Int>()
    var total = 0
    var distinctSum = 0
    for (x in nums) { total += x; if (distinct.add(x)) distinctSum += x }
    return (3 * distinctSum - total) / 2
}
