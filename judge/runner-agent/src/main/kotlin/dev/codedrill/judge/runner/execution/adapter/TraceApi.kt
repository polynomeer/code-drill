package dev.codedrill.judge.runner.execution.adapter

import dev.codedrill.judge.protocol.TraceEventType

/**
 * 계측 SDK 표면 (기술 설계서 §0.3, §7.3 Instrumentation).
 *
 * 세 언어의 `Drill` 이 여기서 생성된다. 목록을 한 곳에 두는 이유는 단순한 중복 제거가
 * 아니다 — 언어마다 손으로 적으면 **같은 이름의 메서드가 언어별로 다른 이벤트를 내는**
 * 상황이 생기고, 그러면 같은 풀이가 언어에 따라 다른 리플레이를 보여준다.
 *
 * 새 자료구조를 지원하려면 여기에 한 줄 추가하고 세 어댑터의 `emit` 만 있으면 된다.
 */
object TraceApi {

    enum class SdkType { INT, STRING }

    data class Param(val name: String, val type: SdkType, val default: String? = null)

    /**
     * [ref] 와 [after] 는 파라미터 이름이다. 각 언어 어댑터가 그 값을 문자열로 바꿔
     * 이벤트 봉투의 `targetRef` / `after` 에 싣는다.
     */
    data class Method(
        val name: String,
        val params: List<Param>,
        val eventType: TraceEventType,
        val ref: String,
        val after: String? = null,
    )

    private fun int(name: String, default: String? = null) = Param(name, SdkType.INT, default)
    private fun str(name: String) = Param(name, SdkType.STRING)

    /** 상수 참조. 스택의 top 처럼 대상이 고정된 이벤트에 쓴다. */
    const val LITERAL_PREFIX = "'"

    val methods: List<Method> = listOf(
        // 배열과 포인터
        Method("visit", listOf(int("index"), int("value", "0")), TraceEventType.VISIT, "index", "value"),
        Method("compare", listOf(int("left"), int("right")), TraceEventType.COMPARE, "left", "right"),
        Method("swap", listOf(int("left"), int("right")), TraceEventType.SWAP, "left", "right"),
        Method("write", listOf(int("index"), int("value")), TraceEventType.WRITE, "index", "value"),
        Method("pointer", listOf(str("name"), int("index")), TraceEventType.POINTER, "index", "name"),

        // 스택
        Method("push", listOf(int("value")), TraceEventType.PUSH, "${LITERAL_PREFIX}top", "value"),
        Method("pop", listOf(int("value")), TraceEventType.POP, "${LITERAL_PREFIX}top", "value"),

        // 큐
        Method("enqueue", listOf(int("value")), TraceEventType.ENQUEUE, "${LITERAL_PREFIX}tail", "value"),
        Method("dequeue", listOf(int("value")), TraceEventType.DEQUEUE, "${LITERAL_PREFIX}head", "value"),

        // 그래프
        Method("node", listOf(str("id")), TraceEventType.NODE, "id"),
        // start/end 로 둔다. from 은 Python 예약어라 SDK 자체가 문법 오류가 된다.
        Method("edge", listOf(str("start"), str("end")), TraceEventType.EDGE, "start", "end"),

        // 재귀
        Method("call", listOf(str("label")), TraceEventType.CALL, "label"),
        Method("ret", listOf(str("label"), int("result")), TraceEventType.RETURN, "label", "result"),

        // 공통
        Method("match", listOf(int("left"), int("right")), TraceEventType.MATCH, "left", "right"),
    )

    /**
     * 세 언어의 예약어.
     *
     * SDK 이름이 여기에 걸리면 그 언어에서 **SDK 파일 자체가 문법 오류**가 되고, 사용자
     * 코드는 컴파일 실패로 보인다. 자기 코드에 문제가 없는데 채점이 거부하는 상황이라
     * 원인을 찾기가 특히 어렵다.
     */
    val reservedWords: Set<String> = setOf(
        // Kotlin
        "as", "break", "class", "continue", "do", "else", "false", "for", "fun", "if", "in",
        "interface", "is", "null", "object", "package", "return", "super", "this", "throw",
        "true", "try", "typealias", "typeof", "val", "var", "when", "while",
        // Java
        "abstract", "assert", "boolean", "byte", "case", "catch", "char", "const", "default",
        "double", "enum", "extends", "final", "finally", "float", "goto", "implements",
        "import", "instanceof", "int", "long", "native", "new", "private", "protected",
        "public", "short", "static", "strictfp", "switch", "synchronized", "throws",
        "transient", "void", "volatile",
        // Python
        "and", "async", "await", "del", "elif", "except", "from", "global", "lambda",
        "nonlocal", "not", "or", "pass", "raise", "with", "yield", "None", "True", "False",
    )

    /** 참조가 리터럴인지. 리터럴이면 파라미터가 아니라 고정 문자열이다. */
    fun isLiteral(reference: String) = reference.startsWith(LITERAL_PREFIX)

    fun literalValue(reference: String) = reference.removePrefix(LITERAL_PREFIX)
}
