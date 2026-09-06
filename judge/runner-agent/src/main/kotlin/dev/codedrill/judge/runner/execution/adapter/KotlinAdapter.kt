package dev.codedrill.judge.runner.execution.adapter

import dev.codedrill.judge.protocol.ExecutionMode
import dev.codedrill.judge.protocol.ExecutionRequest
import dev.codedrill.judge.protocol.Language
import dev.codedrill.judge.protocol.RequestedGroup
import dev.codedrill.judge.runner.execution.KotlinSourceCompiler
import dev.codedrill.judge.runner.execution.RuntimeClasspath
import dev.codedrill.judge.runner.execution.qualifiedId
import dev.codedrill.platform.problempackage.Signature
import dev.codedrill.platform.problempackage.ValueType
import java.io.File
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.writeText

/**
 * Kotlin 런타임 어댑터 (기술 설계서 §5.4).
 *
 * 사용자는 최상위 함수를 작성한다. 하네스와 계측 SDK 가 같은 루트 패키지에서 함께
 * 컴파일되므로, 사용자가 `main` 이나 하네스 심볼과 충돌하는 이름을 쓰면 컴파일 실패로
 * 이어지고 이는 사용자 코드 오류로 분류된다.
 */
class KotlinAdapter(
    private val compiler: KotlinSourceCompiler = KotlinSourceCompiler(RuntimeClasspath.all),
) : RuntimeAdapter {

    override val language = Language.KOTLIN

    override fun prepare(request: ExecutionRequest, sourceDir: Path) {
        sourceDir.resolve("Solution.kt").writeText(request.source)
        sourceDir.resolve("Drill.kt").writeText(drill(request.mode))
        sourceDir.resolve("Main.kt").writeText(harness(request.signature, request.groups))
        for (group in request.groups) {
            sourceDir.resolve(caseFileName(group.policy.id)).writeText(
                group.cases.joinToString("\n") {
                    encodeCaseLine(it.qualifiedId(), request.signature.parameters, it.args)
                },
            )
        }
    }

    override fun compile(sourceDir: Path, outputDir: Path): RuntimeAdapter.CompileOutcome =
        compiler.compile(sourceDir, outputDir)

    override fun command(
        sourceDir: Path,
        outputDir: Path,
        groupId: String,
        memoryMb: Int,
    ): List<String> = listOf(
        "java",
        "-Xmx${memoryMb}m",
        // 힙을 다 쓰면 즉시 OOM 으로 끝내 MEMORY_LIMIT 판정을 결정적으로 만든다.
        "-XX:+UseSerialGC",
        "-XX:-UsePerfData",
        "-Dfile.encoding=UTF-8",
        "-cp",
        (RuntimeClasspath.all.plusElement(outputDir)).joinToString(File.pathSeparator) {
            it.absolutePathString()
        },
        "MainKt",
        groupId,
        sourceDir.absolutePathString(),
    )

    override fun readOnlyPaths(): List<Path> = RuntimeClasspath.all

    // --- 코드 생성 ---

    private fun drill(mode: ExecutionMode): String = if (!mode.instrumented()) {
        // 공식 판정 실행용 no-op 계측 (§7.1). 시그니처는 계측 버전과 같아야 한다 —
        // 다르면 계측을 넣은 코드가 채점에서만 컴파일 실패한다.
        "object Drill {\n" + methods(instrumented = false) + "\n}"
    } else {
        buildString {
            appendLine("import java.io.PrintStream")
            appendLine()
            appendLine("object Drill {")
            appendLine("    private val out: PrintStream = __cdProtocolStream()")
            appendLine("    private var seq = 0L")
            appendLine("    private var budgetLeft = $EVENT_BUDGET")
            appendLine()
            appendLine("    private fun emit(type: String, ref: String, after: String, importance: Int) {")
            appendLine("        if (budgetLeft <= 0) return")
            appendLine("        budgetLeft -= 1")
            appendLine("        seq += 1")
            appendLine("        out.println(")
            appendLine("            \"${SandboxProtocol.EVENT}\\t\" + seq + \"\\t\" + type + \"\\t\" + ref +")
            appendLine("                \"\\t\\t\" + after + \"\\t\" + importance")
            appendLine("        )")
            appendLine("        out.flush()")
            appendLine("    }")
            appendLine()
            appendLine(methods(instrumented = true))
            appendLine("}")
        }
    }

    /** 계측 메서드는 [TraceApi] 에서 생성한다. 세 언어가 같은 표면을 갖게 하는 장치다. */
    private fun methods(instrumented: Boolean): String = TraceApi.methods.joinToString("\n") { method ->
        val params = method.params.joinToString(", ") { param ->
            val type = if (param.type == TraceApi.SdkType.INT) "Int" else "String"
            "${param.name}: $type" + (param.default?.let { " = $it" } ?: "")
        }
        if (!instrumented) {
            "    fun ${method.name}($params) {}"
        } else {
            "    fun ${method.name}($params) = " +
                "emit(\"${method.eventType}\", ${expr(method.ref)}, ${expr(method.after)}, " +
                "${method.eventType.defaultImportance})"
        }
    }

    /** 파라미터 이름이면 문자열로, 리터럴이면 따옴표로 감싼다. */
    private fun expr(reference: String?): String = when {
        reference == null -> "\"\""
        TraceApi.isLiteral(reference) -> "\"${TraceApi.literalValue(reference)}\""
        else -> "$reference.toString()"
    }

    private fun harness(signature: Signature, groups: List<RequestedGroup>): String = buildString {
        appendLine("import java.io.OutputStream")
        appendLine("import java.io.PrintStream")
        appendLine()
        // 계측 SDK 가 사용자 출력 리다이렉션 이전의 원본 스트림을 잡을 수 있도록 연다.
        appendLine("internal val __cdProtocol: PrintStream = System.out")
        appendLine("internal fun __cdProtocolStream(): PrintStream = __cdProtocol")
        appendLine()
        // 사용자 출력은 세기만 하고 버린다. 무제한 버퍼링은 그 자체로 OOM 을 유발해
        // OUTPUT_LIMIT 이어야 할 상황을 MEMORY_LIMIT 으로 오분류한다.
        appendLine("private class __cdCounting : OutputStream() {")
        appendLine("    var total: Long = 0")
        appendLine("    override fun write(b: Int) { total += 1 }")
        appendLine("    override fun write(b: ByteArray, off: Int, len: Int) { total += len }")
        appendLine("}")
        appendLine()
        appendLine("private val __cdUserOut = __cdCounting()")
        appendLine()
        appendLine("private fun __cdEncode(v: IntArray): String = v.joinToString(\",\")")
        appendLine("private fun __cdEncode(v: Int): String = v.toString()")
        appendLine()
        appendLine("private fun __cdCase(id: String, body: () -> String) {")
        // START 를 먼저 흘려보내야, 데드라인으로 프로세스를 죽여도 어느 케이스에서
        // 멈췄는지 특정할 수 있다 (§4.4).
        appendLine("    __cdProtocol.println(\"${SandboxProtocol.START}\\t\" + id)")
        appendLine("    __cdProtocol.flush()")
        appendLine("    val started = System.nanoTime()")
        appendLine("    val outcome = try {")
        appendLine("        \"OK\\t\" + body()")
        appendLine("    } catch (t: Throwable) {")
        appendLine("        \"ERROR\\t\" + (t::class.qualifiedName ?: \"Throwable\")")
        appendLine("    }")
        appendLine("    val elapsedMs = (System.nanoTime() - started) / 1_000_000")
        appendLine("    val runtime = Runtime.getRuntime()")
        appendLine("    val used = runtime.totalMemory() - runtime.freeMemory()")
        appendLine("    __cdProtocol.println(")
        appendLine("        \"${SandboxProtocol.RESULT}\\t\" + id + \"\\t\" + outcome + \"\\t\" + elapsedMs +")
        appendLine("            \"\\t\" + used + \"\\t\" + __cdUserOut.total")
        appendLine("    )")
        appendLine("    __cdProtocol.flush()")
        appendLine("}")
        appendLine()
        appendLine("private fun __cdInts(field: String): IntArray =")
        appendLine("    if (field.isEmpty()) IntArray(0)")
        appendLine("    else field.split(\",\").map { it.toInt() }.toIntArray()")
        appendLine()
        appendLine("fun main(args: Array<String>) {")
        appendLine("    val group = args[0]")
        appendLine("    val dir = java.io.File(args[1])")
        appendLine("    System.setOut(PrintStream(__cdUserOut, true))")
        appendLine("    val lines = java.io.File(dir, \"cases_\" + group + \".txt\").readLines()")
        appendLine("    for (line in lines) {")
        appendLine("        if (line.isEmpty()) continue")
        appendLine("        val f = line.split('\\t')")
        appendLine("        __cdCase(f[0]) { __cdEncode(" + call(signature) + ") }")
        appendLine("    }")
        appendLine("    __cdProtocol.println(\"${SandboxProtocol.DONE}\")")
        appendLine("    __cdProtocol.flush()")
        appendLine("}")
    }

    /** 시그니처대로 필드를 풀어 사용자 함수를 부르는 표현식. */
    private fun call(signature: Signature): String {
        val args = signature.parameters.mapIndexed { index, parameter ->
            when (parameter.type) {
                ValueType.INT -> "f[${index + 1}].toInt()"
                ValueType.INT_ARRAY -> "__cdInts(f[${index + 1}])"
            }
        }
        return "${signature.name}(${args.joinToString(", ")})"
    }

    private fun quote(value: String) = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

    private companion object {
        const val EVENT_BUDGET = 1_000
    }
}
