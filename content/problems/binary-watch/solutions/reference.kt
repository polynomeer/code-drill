// 검증용 정답 (§6.1 solutions/). 시 0..11 과 분 0..59 를 이른 순서로 돌며 켜진 비트 수의 합이 lit 인 것을 담는다.
fun binaryWatch(lit: Int): Array<String> {
    val out = ArrayList<String>()
    for (hour in 0 until 12) for (minute in 0 until 60) {
        if (Integer.bitCount(hour) + Integer.bitCount(minute) == lit) {
            val time = "$hour:" + (if (minute < 10) "0" else "") + minute
            out.add(time)
            Drill.call(time)
        }
    }
    return out.toTypedArray()
}
