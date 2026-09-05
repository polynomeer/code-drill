package dev.codedrill.judge.runner.execution

import dev.codedrill.judge.protocol.ExecutionMode
import dev.codedrill.judge.protocol.TraceCapture
import dev.codedrill.judge.protocol.TraceEvent
import dev.codedrill.judge.protocol.TraceEventType

/**
 * 계측 SDK 소스 생성 (기술 설계서 §0.3, §7.3).
 *
 * 임의의 코드에서 의미 이벤트를 자동 추론하지 않는다. 사용자가 문제별 계측 규약에 따라
 * `Drill.visit(i)` 같은 호출을 직접 넣고, Runner 는 모드에 따라 다른 `Drill` 구현을 함께
 * 컴파일한다.
 *
 * - [ExecutionMode.JUDGE] 는 **아무것도 하지 않는** 구현이다. 공식 판정 실행에 계측
 *   오버헤드를 넣지 않는다는 §7.1 의 요구가 이 한 줄로 지켜진다.
 * - [ExecutionMode.TRACE] 는 이벤트를 프로토콜 스트림으로 흘린다.
 *
 * 어느 모드에서든 같은 사용자 코드가 컴파일된다. 계측 호출이 있다고 채점에서 컴파일이
 * 깨지면 사용자는 계측을 지워버릴 것이다.
 */
object TraceSdk {

    const val FILE_NAME = "Drill.kt"

    /** 하네스와 SDK 가 공유하는 이벤트 줄 접두사. */
    const val EVENT_PREFIX = "EVENT"

    fun source(mode: ExecutionMode): String = when (mode) {
        ExecutionMode.JUDGE -> NO_OP
        ExecutionMode.TRACE -> RECORDING
    }

    /**
     * 프로토콜 줄을 [TraceEvent] 로 되돌린다.
     *
     * `EVENT <seq> <type> <target> <before> <after> <importance>`
     */
    fun parse(line: String): TraceEvent? {
        val parts = line.split('\t')
        if (parts.size < 7 || parts[0] != EVENT_PREFIX) return null
        val type = runCatching { TraceEventType.valueOf(parts[2]) }.getOrNull() ?: return null
        val seq = parts[1].toLongOrNull() ?: return null
        return TraceEvent(
            seq = seq,
            // 슬라이스는 논리 시각을 순번과 같이 둔다. 병렬 실행이 들어오면 갈라져야 한다.
            logicalTime = seq,
            eventType = type,
            target = parts[3],
            before = parts[4].takeIf { it.isNotEmpty() },
            after = parts[5].takeIf { it.isNotEmpty() },
            importance = parts[6].toIntOrNull() ?: 1,
        )
    }

    private val NO_OP = """
        // 공식 판정 실행용 no-op 계측 (§7.1).
        object Drill {
            fun visit(index: Int, value: Int = 0) {}
            fun compare(left: Int, right: Int) {}
            fun match(left: Int, right: Int) {}
        }
    """.trimIndent()

    private val RECORDING = """
        // 학습용 트레이스 계측 (§7.3 Instrumentation).
        import java.io.PrintStream

        object Drill {
            private val out: PrintStream = __cdProtocolStream()
            private var seq = 0L
            private var budgetLeft = ${TraceCapture.EVENT_BUDGET}

            private fun emit(type: String, target: String, before: String, after: String, importance: Int) {
                if (budgetLeft <= 0) return
                budgetLeft -= 1
                seq += 1
                out.println("$EVENT_PREFIX\t" + seq + "\t" + type + "\t" + target + "\t" + before + "\t" + after + "\t" + importance)
                out.flush()
            }

            fun visit(index: Int, value: Int = 0) = emit("VISIT", "array:" + index, "", value.toString(), 1)
            fun compare(left: Int, right: Int) = emit("COMPARE", "array:" + left, "", right.toString(), 2)
            fun match(left: Int, right: Int) = emit("MATCH", "array:" + left, "", right.toString(), 3)

            fun exhausted(): Boolean = budgetLeft <= 0
        }
    """.trimIndent()
}
