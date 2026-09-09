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
    /**
     * 컴파일과 실행에 함께 붙는 Kotlin 런타임 jar.
     *
     * 주입받는 이유는 하나뿐이다 — Runner 가 컨테이너 안에서 돌면 이 경로가 호스트와
     * 같아야 한다 ([RuntimeClasspath.sharedInto]).
     */
    private val runtime: List<Path> = RuntimeClasspath.all,
    private val compiler: KotlinSourceCompiler = KotlinSourceCompiler(runtime),
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
        (runtime.plusElement(outputDir)).joinToString(File.pathSeparator) {
            it.absolutePathString()
        },
        "MainKt",
        groupId,
        sourceDir.absolutePathString(),
    )

    override fun readOnlyPaths(): List<Path> = runtime

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
        // 반환 타입마다 이름을 다르게 둔다. 오버로드로 두면 사용자 함수의 반환 타입이
        // 조금만 달라도 해소가 흔들리고, 그 실패는 사용자에게 컴파일 오류로 보인다.
        appendLine("private fun __cdB64(v: String): String =")
        appendLine("    java.util.Base64.getEncoder().encodeToString(v.toByteArray(Charsets.UTF_8))")
        appendLine("private fun __cdEncodeInt(v: Int): String = v.toString()")
        appendLine("private fun __cdEncodeInts(v: IntArray): String = v.joinToString(\",\")")
        appendLine("private fun __cdEncodeStr(v: String): String = __cdB64(v)")
        appendLine("private fun __cdEncodeStrs(v: Array<String>): String =")
        appendLine("    (listOf(v.size.toString()) + v.map { __cdB64(it) }).joinToString(\",\")")
        appendLine("private fun __cdEncodeGrid(v: Array<IntArray>): String {")
        appendLine("    val cols = if (v.isEmpty()) 0 else v[0].size")
        appendLine("    val out = StringBuilder().append(v.size).append(',').append(cols)")
        appendLine("    for (row in v) for (x in row) out.append(',').append(x)")
        appendLine("    return out.toString()")
        appendLine("}")
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
        appendLine("private fun __cdStr(field: String): String =")
        appendLine("    String(java.util.Base64.getDecoder().decode(field), Charsets.UTF_8)")
        appendLine()
        appendLine("private fun __cdStrs(field: String): Array<String> {")
        appendLine("    val parts = field.split(\",\")")
        appendLine("    val count = parts[0].toInt()")
        appendLine("    return Array(count) { __cdStr(parts[it + 1]) }")
        appendLine("}")
        appendLine()
        appendLine("private fun __cdGrid(field: String): Array<IntArray> {")
        appendLine("    val parts = field.split(\",\")")
        appendLine("    val rows = parts[0].toInt()")
        appendLine("    val cols = parts[1].toInt()")
        appendLine("    return Array(rows) { r -> IntArray(cols) { c -> parts[2 + r * cols + c].toInt() } }")
        appendLine("}")
        appendLine()
        appendLine("fun main(args: Array<String>) {")
        appendLine("    val group = args[0]")
        appendLine("    val dir = java.io.File(args[1])")
        appendLine("    System.setOut(PrintStream(__cdUserOut, true))")
        appendLine("    val lines = java.io.File(dir, \"cases_\" + group + \".txt\").readLines()")
        appendLine("    for (line in lines) {")
        appendLine("        if (line.isEmpty()) continue")
        appendLine("        val f = line.split('\\t')")
        appendLine("        __cdCase(f[0]) { ${encoder(signature.returns)}(" + call(signature) + ") }")
        appendLine("    }")
        appendLine("    __cdProtocol.println(\"${SandboxProtocol.DONE}\")")
        appendLine("    __cdProtocol.flush()")
        appendLine("}")
    }

    /** 시그니처대로 필드를 풀어 사용자 함수를 부르는 표현식. */
    private fun call(signature: Signature): String {
        val args = signature.parameters.mapIndexed { index, parameter ->
            val field = "f[${index + 1}]"
            when (parameter.type) {
                ValueType.INT -> "$field.toInt()"
                ValueType.INT_ARRAY -> "__cdInts($field)"
                ValueType.STRING -> "__cdStr($field)"
                ValueType.STRING_ARRAY -> "__cdStrs($field)"
                ValueType.INT_MATRIX -> "__cdGrid($field)"
            }
        }
        return "${signature.name}(${args.joinToString(", ")})"
    }

    private fun encoder(returns: ValueType) = when (returns) {
        ValueType.INT -> "__cdEncodeInt"
        ValueType.INT_ARRAY -> "__cdEncodeInts"
        ValueType.STRING -> "__cdEncodeStr"
        ValueType.STRING_ARRAY -> "__cdEncodeStrs"
        ValueType.INT_MATRIX -> "__cdEncodeGrid"
    }

    private fun quote(value: String) = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

    private companion object {
        const val EVENT_BUDGET = 1_000
    }
}
