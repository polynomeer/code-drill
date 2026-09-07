// 검증용 정답 (§6.1 solutions/).
//
// k 를 먼저 길이로 나눈 나머지로 줄인다. 줄이지 않으면 k 가 클 때 인덱스가 배열을
// 벗어나고, 그 실패는 "가끔 터지는 풀이"로 나타나 원인을 찾기 어렵다.
fun rotate(nums: IntArray, k: Int): IntArray {
    val n = nums.size
    val shift = k % n
    val out = IntArray(n)

    for (index in nums.indices) {
        Drill.visit(index, nums[index])
        val target = (index + shift) % n
        out[target] = nums[index]
        Drill.write(target, nums[index])
    }
    return out
}
