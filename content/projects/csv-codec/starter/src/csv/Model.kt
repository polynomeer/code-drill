package csv

/** 읽을 수 없는 CSV. [offset] 은 문제가 난 글자의 자리(0 부터)다. */
class MalformedCsv(val offset: Int, message: String) : RuntimeException("$message (at $offset)")
