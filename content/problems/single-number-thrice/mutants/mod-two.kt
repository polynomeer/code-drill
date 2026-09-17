// kind: WRONG_BRANCH
// 개수를 2 로 나눈 나머지를 본다. 세 번 나온 수의 비트가 남는다.
fun singleNumberThrice(nums: IntArray): Int {
    var answer = 0
    for (bit in 0 until 32) {
        var count = 0
        for (x in nums) if ((x ushr bit) and 1 == 1) count += 1
        if (count % 2 != 0) answer = answer or (1 shl bit)
    }
    return answer
}
