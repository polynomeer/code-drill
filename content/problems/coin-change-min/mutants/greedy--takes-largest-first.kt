// kind: WRONG_BRANCH
// 큰 동전부터 욕심껏 집는다. [1,3,4] 로 6 을 만들 때 3개를 쓴다.
fun coinChange(coins: IntArray, amount: Int): Int {
    var left = amount
    var used = 0
    for (coin in coins.sortedDescending()) {
        used += left / coin
        left %= coin
    }
    return if (left == 0) used else -1
}
