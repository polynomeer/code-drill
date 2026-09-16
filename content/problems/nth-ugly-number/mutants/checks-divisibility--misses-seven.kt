// kind: WRONG_ALGORITHM
// 2·3·5 중 하나로 나누어떨어지면 못생긴 수로 센다. 14 처럼 다른 소인수가 섞인 수를 센다.
fun nthUglyNumber(n: Int): Int {
    var count = 0
    var x = 0
    while (count < n) {
        x += 1
        if (x == 1 || x % 2 == 0 || x % 3 == 0 || x % 5 == 0) count += 1
    }
    return x
}
