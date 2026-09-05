package dev.codedrill.platform.problempackage

/**
 * 패키지가 스키마·참조 무결성 검사를 통과하지 못했다 (기술 설계서 §6.3).
 *
 * 로더는 경계다. 안쪽에서 나온 파서 예외를 그대로 흘려보내지 않고 이 타입으로 감싸,
 * 호출부가 "깨진 패키지"와 "읽는 중 생긴 IO 오류"를 구분할 수 있게 한다.
 */
class InvalidProblemPackageException(
    problemId: String,
    reason: String,
    cause: Throwable? = null,
) : IllegalArgumentException("문제 패키지가 유효하지 않다 [$problemId]: $reason", cause)
