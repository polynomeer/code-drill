package dev.codedrill.judge.runner.execution.adapter

import dev.codedrill.judge.protocol.ExecutionMode
import dev.codedrill.judge.protocol.ExecutionRequest
import dev.codedrill.judge.protocol.Language
import dev.codedrill.platform.problempackage.ValueType
import java.nio.file.Path

/**
 * 런타임 어댑터 계약 (기술 설계서 §5.3).
 *
 * ```
 * prepare → compile → execute → check → cleanup
 * ```
 *
 * 언어마다 다른 것은 소스 배치·컴파일·실행 명령뿐이다. 하네스가 뱉는 프로토콜과 판정
 * 규칙은 [SandboxProtocol] 로 공유하므로, 새 언어를 붙여도 채점 의미가 갈라지지 않는다.
 *
 * `check` 는 어댑터의 책임이 아니다. 기대 출력은 샌드박스 밖에서만 비교해야 숨은
 * 테스트가 사용자 프로세스로 새지 않는다 (§7.1, §8.3).
 */
interface RuntimeAdapter {

    val language: Language

    /** 사용자 소스·계측 SDK·하네스를 [sourceDir] 에 놓는다. */
    fun prepare(request: ExecutionRequest, sourceDir: Path)

    /**
     * 컴파일한다. 인터프리터 언어는 문법 검사만 수행한다.
     *
     * 실패는 전부 사용자 코드 오류(COMPILE_ERROR)로 다룬다. 플랫폼 문제로 컴파일이
     * 불가능하면 예외를 던져 SYSTEM_ERROR 로 흐르게 한다 (§4.4).
     */
    fun compile(sourceDir: Path, outputDir: Path): CompileOutcome

    /**
     * 샌드박스 안에서 실행할 명령.
     *
     * 경로는 호스트 절대 경로로 만든다. 컨테이너 샌드박스는 같은 경로에 마운트하므로
     * 명령이 두 샌드박스에서 동일하게 동작한다.
     */
    fun command(sourceDir: Path, outputDir: Path, groupId: String, memoryMb: Int): List<String>

    /** 실행에 필요해 샌드박스로 들여보내야 하는 읽기 전용 경로 (런타임 라이브러리 등). */
    fun readOnlyPaths(): List<Path> = emptyList()

    /**
     * 컨테이너 없이도 메모리 상한을 강제할 수 있는지 (§5.2 memory.max).
     *
     * JVM 은 `-Xmx` 로 자기 힙을 스스로 막는다. 인터프리터 언어는 그런 장치가 없어
     * OS 의 rlimit 이나 cgroup 에 기대야 하고, 그마저 없는 플랫폼이 있다.
     *
     * `false` 인 언어를 [dev.codedrill.judge.runner.execution.sandbox.ProcessSandbox] 로
     * 돌리면 **메모리 초과가 잡히지 않는다.** 판정이 MEMORY_LIMIT 대신 TIME_LIMIT 이나
     * 시스템 정지로 나타난다.
     */
    fun enforcesMemoryWithoutContainer(): Boolean = true

    sealed interface CompileOutcome {
        data object Success : CompileOutcome

        /** 사용자에게 그대로 보여줄 수 있는 로그. 서버 절대 경로를 담지 않는다. */
        data class Failure(val log: String) : CompileOutcome
    }
}

/**
 * 하네스와 Runner 가 주고받는 줄 단위 프로토콜.
 *
 * 언어와 무관하게 같은 형식을 쓴다. 형식을 한 곳에 모아 두면 새 언어 어댑터가 판정
 * 의미를 미묘하게 다르게 만드는 일을 막을 수 있다.
 *
 * ```
 * START   <caseId>
 * EVENT   <seq> <type> <target> <before> <after> <importance>
 * RESULT  <caseId> <OK|ERROR> <payload> <elapsedMs> <heapBytes> <userOutBytes>
 * FATAL   <reason>
 * DONE
 * ```
 */
object SandboxProtocol {
    const val START = "START"
    const val EVENT = "EVENT"
    const val RESULT = "RESULT"

    /**
     * 케이스를 하나도 시작하지 못했다.
     *
     * 사용자 코드를 불러오지 못한 경우(파이썬의 import 실패 등)가 여기 온다. 컴파일
     * 단계가 잡지 못한 같은 종류의 실패이므로 COMPILE_ERROR 로 분류한다.
     */
    const val FATAL = "FATAL"

    const val DONE = "DONE"

    /** 기대 출력 비교에 쓰는 인코딩. 모든 언어의 하네스가 이 형식으로 값을 찍는다. */
    fun encode(type: ValueType, value: Any): String = when (type) {
        ValueType.INT -> (value as Number).toInt().toString()
        ValueType.INT_ARRAY -> when (value) {
            is List<*> -> value.joinToString(",") { (it as Number).toInt().toString() }
            else -> error("INT_ARRAY 기대값이 배열이 아니다: $value")
        }
    }
}

/** 계측 SDK 가 모드에 따라 다른 구현으로 컴파일된다는 사실만 공유한다 (§7.1). */
fun ExecutionMode.instrumented(): Boolean = this == ExecutionMode.TRACE

/**
 * 계측 이벤트 줄을 되돌린다 (§7.2).
 *
 * `EVENT <seq> <eventType> <targetRef> <before> <after> <importance>`
 *
 * `targetKind` 는 싣지 않는다. 이벤트 종류가 이미 종류를 결정하므로, 함께 보내면 둘이
 * 어긋날 수 있는 자리를 만드는 셈이다.
 *
 * 세 언어의 SDK 가 모두 이 형식으로 찍는다. 파싱이 한 곳에 있어야 언어를 늘려도
 * 리플레이 클라이언트가 그대로 동작한다.
 */
fun parseTraceEvent(line: String): dev.codedrill.judge.protocol.TraceEvent? {
    val parts = line.split('\t')
    if (parts.size < 7 || parts[0] != SandboxProtocol.EVENT) return null
    val seq = parts[1].toLongOrNull() ?: return null
    val type = runCatching {
        dev.codedrill.judge.protocol.TraceEventType.valueOf(parts[2])
    }.getOrNull() ?: return null

    return dev.codedrill.judge.protocol.TraceEvent(
        seq = seq,
        // 슬라이스는 논리 시각을 순번과 같이 둔다. 병렬 실행이 들어오면 갈라져야 한다.
        logicalTime = seq,
        eventType = type,
        targetKind = type.kind,
        targetRef = parts[3],
        before = parts[4].takeIf { it.isNotEmpty() },
        after = parts[5].takeIf { it.isNotEmpty() },
        importance = parts[6].toIntOrNull() ?: type.defaultImportance,
    )
}

/**
 * 케이스 입력을 담는 파일 이름. 그룹마다 하나씩 만든다.
 *
 * 인자를 하네스 소스에 인라인하면 큰 입력에서 무너진다. JVM 은 메서드 하나의 바이트코드를
 * 64KB 로 제한하므로, 20만 원소짜리 배열 리터럴은 컴파일 자체가 실패한다. 그래서 데이터는
 * 코드 밖에 두고 하네스가 읽는다.
 *
 * 파서는 우리가 만든 고정 형식이라 사용자 입력이 파싱 동작을 바꾸지 못한다.
 * 한 줄이 케이스 하나이고, 필드는 탭으로 나뉜다.
 *
 * ```
 * <caseId>  <arg0>  <arg1> ...
 * ```
 *
 * `INT_ARRAY` 는 쉼표로 이은 정수, `INT` 는 10진수다. 빈 배열은 빈 필드다.
 */
fun caseFileName(groupId: String): String = "cases_$groupId.txt"

/** [caseFileName] 형식으로 케이스 한 줄을 만든다. */
fun encodeCaseLine(
    caseId: String,
    parameters: List<dev.codedrill.platform.problempackage.Parameter>,
    args: List<Any>,
): String = buildString {
    append(caseId)
    parameters.forEachIndexed { index, parameter ->
        append('\t')
        when (parameter.type) {
            ValueType.INT -> append((args[index] as Number).toInt())
            ValueType.INT_ARRAY -> {
                @Suppress("UNCHECKED_CAST")
                val items = args[index] as List<Number>
                append(items.joinToString(",") { it.toInt().toString() })
            }
        }
    }
}
