package booking

/**
 * 회의실 예약 — 골격. 시그니처는 그대로 두고 본문을 채운다.
 *
 * 구간은 전부 반열린 [start, end) 다.
 */
class RoomBooking(val openAt: Int, val closeAt: Int) {
    init {
        TODO("영업 시간을 검사한다")
    }

    fun book(id: String, start: Int, end: Int): Booking {
        TODO()
    }

    fun move(id: String, start: Int, end: Int): Booking {
        TODO()
    }

    fun cancel(id: String): Boolean {
        TODO()
    }

    fun bookings(): List<Booking> {
        TODO()
    }

    fun free(): List<Slot> {
        TODO()
    }
}
