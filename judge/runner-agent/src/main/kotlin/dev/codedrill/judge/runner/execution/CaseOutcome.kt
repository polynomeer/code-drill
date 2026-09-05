package dev.codedrill.judge.runner.execution

/**
 * 자식 JVM 이 케이스 하나에 대해 남긴 관측 결과.
 *
 * 아직 판정이 아니다. 기대 출력과 맞춰보는 일은 checker 단계에서 한다 (§5.3 check).
 * 관측과 판정을 나눠 둬야 checker 정책이 바뀌어도 실행 계층을 건드리지 않는다.
 */
sealed interface CaseOutcome {

    /** 함수가 정상 반환했다. [output] 은 하네스가 인코딩한 문자열이다. */
    data class Completed(
        val output: String,
        val elapsedMillis: Long,
        val heapBytes: Long,
        val userOutputBytes: Long,
    ) : CaseOutcome

    /** 사용자 코드가 예외를 던졌다 → RUNTIME_ERROR. */
    data class Threw(val exceptionClass: String, val elapsedMillis: Long) : CaseOutcome

    /** 제한 시간 안에 끝나지 않아 프로세스를 죽였다 → TIME_LIMIT. */
    data object TimedOut : CaseOutcome

    /** 자식 JVM 이 힙을 다 썼다 → MEMORY_LIMIT. */
    data object MemoryExceeded : CaseOutcome

    /** 출력 한도를 넘겨 스트림을 끊었다 → OUTPUT_LIMIT. */
    data object OutputExceeded : CaseOutcome

    /** 앞선 케이스에서 프로세스가 끝나 실행되지 못했다. */
    data object NotRun : CaseOutcome

    /** 플랫폼 쪽 문제로 관측 자체가 실패했다 → SYSTEM_ERROR (§4.4). */
    data class Broken(val reason: String) : CaseOutcome
}
