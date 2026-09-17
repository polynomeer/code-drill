package booking

/** 예약 하나. 구간은 반열린 [start, end) 다. */
data class Booking(val id: String, val start: Int, val end: Int)

/** 예약이 없는 구간. 반열린 [start, end) 다. */
data class Slot(val start: Int, val end: Int)

class DuplicateBookingException(val id: String) : RuntimeException("booking already exists: $id")

class NoSuchBookingException(val id: String) : RuntimeException("no such booking: $id")

/** 겹치는 예약이 있다. [withId] 가 부딪힌 예약이다. */
class ConflictException(val withId: String) : RuntimeException("conflicts with booking $withId")
