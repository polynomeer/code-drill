// kind: WRONG_ALGORITHM
// 여정마다 좌석에 들어가는지만 본다. 겹치는 여정의 인원은 더해진다.
fun carPooling(trips: IntArray, capacity: Int): Int {
    for (i in trips.indices step 3) if (trips[i] > capacity) return 0
    return 1
}
