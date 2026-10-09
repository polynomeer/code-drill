// kind: WRONG_ALGORITHM
// 지금까지 들어온 호출을 모두 센다. 3000 밀리초 창 안의 호출만 센다.
fun recentCalls(times: IntArray): IntArray = IntArray(times.size) { it + 1 }
