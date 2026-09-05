package dev.codedrill.judge.runner.execution

import dev.codedrill.judge.protocol.RequestedGroup
import dev.codedrill.platform.problempackage.Signature
import dev.codedrill.platform.problempackage.ValueType

/**
 * 함수형 풀이를 실행할 하네스를 생성한다 (기술 설계서 §5.3 prepare).
 *
 * 케이스 인자를 Kotlin 리터럴로 인라인해 컴파일 시점에 고정한다. 자식 JVM 안에서
 * JSON 을 파싱하지 않으므로 파서 자체가 채점 결과에 영향을 줄 여지가 없다.
 *
 * 하네스와 사용자 코드는 같은 루트 패키지에서 함께 컴파일된다. 사용자가 `main` 이나
 * 하네스 심볼과 충돌하는 선언을 하면 컴파일 실패로 이어지고, 이는 사용자 코드 오류
 * (COMPILE_ERROR)로 분류된다.
 */
object HarnessGenerator {

    /** 사용자 코드와 충돌할 확률을 줄이기 위한 접두사. */
    private const val P = "__cd"

    fun generate(signature: Signature, groups: List<RequestedGroup>): String = buildString {
        appendLine("import java.io.OutputStream")
        appendLine("import java.io.PrintStream")
        appendLine()
        appendLine("private val ${P}Protocol: PrintStream = System.out")
        appendLine()
        // 사용자 출력은 세기만 하고 버린다. 무제한 버퍼링은 그 자체로 OOM 을 유발해
        // OUTPUT_LIMIT 이어야 할 상황을 MEMORY_LIMIT 으로 오분류한다.
        appendLine("private class ${P}Counting : OutputStream() {")
        appendLine("    var total: Long = 0")
        appendLine("    override fun write(b: Int) { total += 1 }")
        appendLine("    override fun write(b: ByteArray, off: Int, len: Int) { total += len }")
        appendLine("}")
        appendLine()
        appendLine("private val ${P}UserOut = ${P}Counting()")
        appendLine()
        appendLine("private fun ${P}encode(v: IntArray): String = v.joinToString(\",\")")
        appendLine("private fun ${P}encode(v: Int): String = v.toString()")
        appendLine()
        appendLine("private fun ${P}case(id: String, body: () -> String) {")
        // START 를 먼저 흘려보내야, 부모가 타임아웃으로 프로세스를 죽일 때 어느 케이스에서
        // 멈췄는지 특정할 수 있다 (§4.4 판정 분류).
        appendLine("    ${P}Protocol.println(\"START\\t\" + id)")
        appendLine("    ${P}Protocol.flush()")
        appendLine("    val started = System.nanoTime()")
        appendLine("    val outcome = try {")
        appendLine("        \"OK\\t\" + body()")
        appendLine("    } catch (t: Throwable) {")
        appendLine("        \"ERROR\\t\" + (t::class.qualifiedName ?: \"Throwable\")")
        appendLine("    }")
        appendLine("    val elapsedMs = (System.nanoTime() - started) / 1_000_000")
        appendLine("    val runtime = Runtime.getRuntime()")
        appendLine("    val used = runtime.totalMemory() - runtime.freeMemory()")
        appendLine("    ${P}Protocol.println(")
        appendLine("        \"RESULT\\t\" + id + \"\\t\" + outcome + \"\\t\" + elapsedMs + \"\\t\" + used +")
        appendLine("            \"\\t\" + ${P}UserOut.total")
        appendLine("    )")
        appendLine("    ${P}Protocol.flush()")
        appendLine("}")
        appendLine()
        // 그룹 id 를 인자로 받아 해당 그룹만 실행한다. Runner 가 그룹마다 별도 프로세스를
        // 띄우므로(§6.2 그룹 단위 stop_policy) 한 번 컴파일한 산출물을 그대로 재사용한다.
        appendLine("fun main(args: Array<String>) {")
        appendLine("    val group = args.getOrNull(0)")
        // 사용자의 println 이 프로토콜 스트림을 오염시키지 않도록 분리한다 (§13.3).
        appendLine("    System.setOut(PrintStream(${P}UserOut, true))")
        for (group in groups) {
            appendLine("    if (group == null || group == ${quote(group.policy.id)}) {")
            for (case in group.cases) {
                val call = "${signature.name}(${literals(signature, case.args)})"
                appendLine("        ${P}case(${quote(case.qualifiedId())}) { ${P}encode($call) }")
            }
            appendLine("    }")
        }
        appendLine("    ${P}Protocol.println(\"DONE\")")
        appendLine("    ${P}Protocol.flush()")
        appendLine("}")
    }

    private fun literals(signature: Signature, args: List<Any>): String =
        signature.parameters.mapIndexed { index, parameter ->
            literal(parameter.type, args[index])
        }.joinToString(", ")

    private fun literal(type: ValueType, value: Any): String = when (type) {
        ValueType.INT -> (value as Number).toInt().toString()
        ValueType.INT_ARRAY -> {
            @Suppress("UNCHECKED_CAST")
            val items = value as List<Number>
            "intArrayOf(${items.joinToString(", ") { it.toInt().toString() }})"
        }
    }

    private fun quote(value: String) = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}
